package com.futslot.booking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.ApiException;
import com.futslot.common.PhoneNumbers;
import com.futslot.slot.AvailabilityService;
import com.futslot.slot.BookedRanges;
import com.futslot.slot.CourtBlockRepository;
import com.futslot.slot.Slot;
import com.futslot.slot.SlotStatus;
import com.futslot.user.User;
import com.futslot.user.UserService;
import com.futslot.venue.Court;
import com.futslot.venue.CourtRepository;
import com.futslot.venue.Venue;
import com.futslot.venue.VenueRepository;
import com.futslot.venue.VenueStatus;

/**
 * Creates and changes bookings. Anything that occupies a court first locks the court row
 * ({@link CourtRepository#lockById}), then expires lapsed holds on it, then checks for conflicts, so two
 * requests for the same slot are serialised. The database's exclusion constraint is the final backstop.
 */
@Service
public class BookingService {

	static final int MAX_ACTIVE_HOLDS_PER_PLAYER = 3;

	private static final Duration MAX_MANUAL_DURATION = Duration.ofHours(12);

	private static final List<BookingStatus> LIVE = List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED);

	private final BookingRepository bookings;

	private final VenueCustomerRepository customers;

	private final VenueRepository venues;

	private final CourtRepository courts;

	private final CourtBlockRepository blocks;

	private final BookedRanges bookedRanges;

	private final AvailabilityService availability;

	private final UserService userService;

	private final BookingReferences references;

	private final JdbcClient jdbc;

	private final Clock clock;

	BookingService(BookingRepository bookings, VenueCustomerRepository customers, VenueRepository venues,
			CourtRepository courts, CourtBlockRepository blocks, BookedRanges bookedRanges,
			AvailabilityService availability, UserService userService, BookingReferences references, JdbcClient jdbc,
			Clock clock) {
		this.bookings = bookings;
		this.customers = customers;
		this.venues = venues;
		this.courts = courts;
		this.blocks = blocks;
		this.bookedRanges = bookedRanges;
		this.availability = availability;
		this.userService = userService;
		this.references = references;
		this.jdbc = jdbc;
		this.clock = clock;
	}

	@Transactional
	public BookingResponse bookAsPlayer(UUID userId, PlayerBookingRequest request) {
		Court court = this.courts.findById(request.courtId()).filter(Court::isActive).orElseThrow(BookingService::courtNotFound);
		Venue venue = court.getVenue();
		if (venue.getStatus() != VenueStatus.ACTIVE) {
			throw courtNotFound();
		}
		Instant now = this.clock.instant();
		Instant start = request.startsAt().toInstant();
		lockAndExpireHolds(court, now);

		DatedSlot dated = findSlot(venue, court, start)
			.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "SLOT_NOT_FOUND", "That isn't a bookable slot"));
		LocalDate lastBookable = LocalDate.now(this.clock.withZone(zone(venue)))
			.plusDays(venue.policy().maxAdvanceBookingDays());
		if (dated.date().isAfter(lastBookable)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "DATE_OUT_OF_RANGE", "Bookings open up to " + lastBookable);
		}
		switch (dated.slot().status()) {
			case PAST -> throw new ApiException(HttpStatus.CONFLICT, "SLOT_IN_PAST", "That slot has already started");
			case BOOKED, BLOCKED -> throw slotUnavailable();
			case AVAILABLE -> {
			}
		}
		if (this.bookings.countActiveHolds(userId, now) >= MAX_ACTIVE_HOLDS_PER_PLAYER) {
			throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_HOLDS",
					"Pay for or cancel your pending bookings before holding more slots");
		}

		User user = this.userService.get(userId);
		VenueCustomer customer = customerFor(venue, user.getPhone(), user.getFullName());
		customer.linkUser(userId);
		Slot slot = dated.slot();
		Booking booking = new Booking(this.references.next(), venue, court, customer, userId, BookingSource.APP,
				start, slot.endsAt().toInstant(), slot.price(), userId, request.notes());
		int advancePercent = venue.policy().advancePercent();
		if (advancePercent > 0) {
			BigDecimal advance = slot.price()
				.multiply(BigDecimal.valueOf(advancePercent))
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
			booking.holdForPayment(advance, now.plus(Duration.ofMinutes(venue.policy().bookingHoldMinutes())));
		}
		return BookingResponse.from(save(booking, customer, now));
	}

	@Transactional
	public BookingResponse bookAsOwner(UUID ownerId, UUID venueId, OwnerBookingRequest request) {
		Venue venue = ownedVenue(ownerId, venueId);
		if (request.source() != BookingSource.WALK_IN && request.source() != BookingSource.PHONE) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SOURCE", "Source must be WALK_IN or PHONE");
		}
		Court court = this.courts.findByIdAndVenueId(request.courtId(), venueId)
			.filter(Court::isActive)
			.orElseThrow(BookingService::courtNotFound);
		Instant now = this.clock.instant();
		Instant start = request.startsAt().toInstant();
		Instant end = (request.endsAt() != null) ? request.endsAt().toInstant()
				: start.plus(Duration.ofMinutes(court.getSlotDurationMinutes()));
		validateRange(start, end);
		lockAndExpireHolds(court, now);
		ensureFree(court, start, end, now, null);

		BigDecimal price = (request.price() != null) ? request.price() : priceOfSlots(venue, court, start, end);
		VenueCustomer customer = customerFor(venue, PhoneNumbers.normalize(request.customerPhone()),
				request.customerName().strip());
		Booking booking = new Booking(this.references.next(), venue, court, customer, null, request.source(), start,
				end, price, ownerId, request.notes());
		return BookingResponse.from(save(booking, customer, now));
	}

	@Transactional
	public BookingResponse reschedule(UUID ownerId, UUID venueId, UUID bookingId, RescheduleRequest request) {
		ownedVenue(ownerId, venueId);
		Booking booking = venueBooking(venueId, bookingId);
		requireLive(booking);
		Court court = (request.courtId() != null) ? this.courts.findByIdAndVenueId(request.courtId(), venueId)
			.filter(Court::isActive)
			.orElseThrow(BookingService::courtNotFound) : booking.getCourt();
		Instant now = this.clock.instant();
		Instant start = request.startsAt().toInstant();
		Instant end = (request.endsAt() != null) ? request.endsAt().toInstant()
				: start.plus(Duration.between(booking.getStartsAt(), booking.getEndsAt()));
		validateRange(start, end);
		lockAndExpireHolds(court, now);
		ensureFree(court, start, end, now, booking.getId());
		booking.moveTo(court, start, end);
		flush();
		return BookingResponse.from(booking);
	}

	@Transactional
	public BookingResponse cancelAsPlayer(UUID userId, UUID bookingId, String reason) {
		Booking booking = this.bookings.findByIdAndBookedByUserId(bookingId, userId)
			.orElseThrow(BookingService::bookingNotFound);
		requireLive(booking);
		Instant now = this.clock.instant();
		if (!now.isBefore(booking.getStartsAt())) {
			throw new ApiException(HttpStatus.CONFLICT, "CANCELLATION_CLOSED",
					"This booking has started. Contact the venue");
		}
		booking.cancel(userId, reason, now);
		return BookingResponse.from(booking);
	}

	@Transactional
	public BookingResponse cancelAsOwner(UUID ownerId, UUID venueId, UUID bookingId, String reason) {
		ownedVenue(ownerId, venueId);
		Booking booking = venueBooking(venueId, bookingId);
		requireLive(booking);
		booking.cancel(ownerId, reason, this.clock.instant());
		return BookingResponse.from(booking);
	}

	@Transactional
	public BookingResponse markNoShow(UUID ownerId, UUID venueId, UUID bookingId) {
		Booking booking = startedConfirmedBooking(ownerId, venueId, bookingId);
		booking.markNoShow();
		return BookingResponse.from(booking);
	}

	@Transactional
	public BookingResponse markCompleted(UUID ownerId, UUID venueId, UUID bookingId) {
		Booking booking = startedConfirmedBooking(ownerId, venueId, bookingId);
		booking.markCompleted();
		return BookingResponse.from(booking);
	}

	@Transactional(readOnly = true)
	public List<BookingResponse> listForPlayer(UUID userId, boolean upcoming, int page, int size) {
		Instant now = this.clock.instant();
		PageRequest pageable = PageRequest.of(page, size);
		List<Booking> result = upcoming ? this.bookings.findUpcomingForUser(userId, now, LIVE, pageable)
				: this.bookings.findPastForUser(userId, now, LIVE, pageable);
		return result.stream().map(BookingResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public BookingResponse getForPlayer(UUID userId, UUID bookingId) {
		return this.bookings.findByIdAndBookedByUserId(bookingId, userId)
			.map(BookingResponse::from)
			.orElseThrow(BookingService::bookingNotFound);
	}

	/** Every booking touching the venue's local calendar day, cancelled ones included. */
	@Transactional(readOnly = true)
	public List<BookingResponse> listForVenueDay(UUID ownerId, UUID venueId, LocalDate date) {
		Venue venue = ownedVenue(ownerId, venueId);
		ZoneId zone = zone(venue);
		return this.bookings
			.findForVenueBetween(venueId, date.atStartOfDay(zone).toInstant(), date.plusDays(1).atStartOfDay(zone).toInstant())
			.stream()
			.map(BookingResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public BookingResponse getForOwner(UUID ownerId, UUID venueId, UUID bookingId) {
		ownedVenue(ownerId, venueId);
		return BookingResponse.from(venueBooking(venueId, bookingId));
	}

	private void lockAndExpireHolds(Court court, Instant now) {
		this.courts.lockById(court.getId());
		BookingMaintenance.expireLapsedHolds(this.jdbc, court.getId(), now);
	}

	/** Finds the slot starting at {@code start}; late-night slots belong to the previous operating day. */
	private Optional<DatedSlot> findSlot(Venue venue, Court court, Instant start) {
		LocalDate localDate = start.atZone(zone(venue)).toLocalDate();
		for (LocalDate date : List.of(localDate, localDate.minusDays(1))) {
			Optional<Slot> slot = this.availability.slotsFor(venue, court, date)
				.stream()
				.filter(candidate -> candidate.startsAt().toInstant().equals(start))
				.findFirst();
			if (slot.isPresent()) {
				return Optional.of(new DatedSlot(date, slot.get()));
			}
		}
		return Optional.empty();
	}

	/** Sum of slot prices when [start, end) is exactly a run of consecutive slots. */
	private BigDecimal priceOfSlots(Venue venue, Court court, Instant start, Instant end) {
		LocalDate localDate = start.atZone(zone(venue)).toLocalDate();
		for (LocalDate date : List.of(localDate, localDate.minusDays(1))) {
			List<Slot> covered = this.availability.slotsFor(venue, court, date)
				.stream()
				.filter(slot -> !slot.startsAt().toInstant().isBefore(start) && !slot.endsAt().toInstant().isAfter(end))
				.sorted(Comparator.comparing(Slot::startsAt))
				.toList();
			if (tiles(covered, start, end)) {
				return covered.stream().map(Slot::price).reduce(BigDecimal.ZERO, BigDecimal::add);
			}
		}
		throw new ApiException(HttpStatus.BAD_REQUEST, "PRICE_REQUIRED",
				"These times don't match the court's priced slots; enter a price");
	}

	private static boolean tiles(List<Slot> slots, Instant start, Instant end) {
		if (slots.isEmpty() || !slots.getFirst().startsAt().toInstant().equals(start)
				|| !slots.getLast().endsAt().toInstant().equals(end)) {
			return false;
		}
		for (int i = 1; i < slots.size(); i++) {
			if (!slots.get(i - 1).endsAt().equals(slots.get(i).startsAt())) {
				return false;
			}
		}
		return true;
	}

	private void ensureFree(Court court, Instant start, Instant end, Instant now, UUID ignoreBookingId) {
		if (!this.blocks.findOverlapping(List.of(court.getId()), start, end).isEmpty()) {
			throw new ApiException(HttpStatus.CONFLICT, "SLOT_BLOCKED", "The court is blocked at that time");
		}
		if (!this.bookedRanges.byCourt(List.of(court.getId()), start, end, now, ignoreBookingId).isEmpty()) {
			throw slotUnavailable();
		}
	}

	private Booking save(Booking booking, VenueCustomer customer, Instant now) {
		customer.recordBooking(now);
		this.bookings.save(booking);
		flush();
		return booking;
	}

	private void flush() {
		try {
			this.bookings.flush();
		}
		catch (DataIntegrityViolationException ex) {
			// Only reachable if a conflicting write bypassed the court lock; the constraint still holds
			throw slotUnavailable();
		}
	}

	private VenueCustomer customerFor(Venue venue, String phone, String name) {
		return this.customers.findByVenueIdAndPhone(venue.getId(), phone)
			.orElseGet(() -> this.customers.save(new VenueCustomer(venue.getId(), phone, name)));
	}

	private Booking startedConfirmedBooking(UUID ownerId, UUID venueId, UUID bookingId) {
		ownedVenue(ownerId, venueId);
		Booking booking = venueBooking(venueId, bookingId);
		if (booking.getStatus() != BookingStatus.CONFIRMED) {
			throw invalidStatus(booking);
		}
		if (this.clock.instant().isBefore(booking.getStartsAt())) {
			throw new ApiException(HttpStatus.CONFLICT, "BOOKING_NOT_STARTED", "This booking hasn't started yet");
		}
		return booking;
	}

	private Venue ownedVenue(UUID ownerId, UUID venueId) {
		return this.venues.findByIdAndOwnerId(venueId, ownerId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VENUE_NOT_FOUND", "Venue not found"));
	}

	private Booking venueBooking(UUID venueId, UUID bookingId) {
		return this.bookings.findByIdAndVenueId(bookingId, venueId).orElseThrow(BookingService::bookingNotFound);
	}

	private static void requireLive(Booking booking) {
		if (!booking.isLive()) {
			throw invalidStatus(booking);
		}
	}

	private static void validateRange(Instant start, Instant end) {
		if (!end.isAfter(start) || Duration.between(start, end).compareTo(MAX_MANUAL_DURATION) > 0) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE",
					"End must be after start and within 12 hours");
		}
	}

	private static ZoneId zone(Venue venue) {
		return ZoneId.of(venue.getTimezone());
	}

	private static ApiException slotUnavailable() {
		return new ApiException(HttpStatus.CONFLICT, "SLOT_UNAVAILABLE", "Sorry, that slot was just taken");
	}

	private static ApiException courtNotFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "COURT_NOT_FOUND", "Court not found");
	}

	private static ApiException bookingNotFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "Booking not found");
	}

	private static ApiException invalidStatus(Booking booking) {
		return new ApiException(HttpStatus.CONFLICT, "INVALID_BOOKING_STATUS",
				"Not possible for a booking that is " + booking.getStatus());
	}

	private record DatedSlot(LocalDate date, Slot slot) {

	}

}
