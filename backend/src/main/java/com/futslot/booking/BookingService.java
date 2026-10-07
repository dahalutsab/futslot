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

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.ApiException;
import com.futslot.common.PhoneNumbers;
import com.futslot.discount.DiscountCode;
import com.futslot.discount.DiscountCodeService;
import com.futslot.membership.Membership;
import com.futslot.membership.MembershipService;
import com.futslot.slot.AvailabilityService;
import com.futslot.slot.BookedRanges;
import com.futslot.slot.CourtBlockRepository;
import com.futslot.slot.Slot;
import com.futslot.slot.SlotStatus;
import com.futslot.user.User;
import com.futslot.user.UserRepository;
import com.futslot.user.UserService;
import com.futslot.venue.AddOnService;
import com.futslot.venue.Court;
import com.futslot.venue.CourtRepository;
import com.futslot.venue.Venue;
import com.futslot.venue.VenueAccess;
import com.futslot.venue.VenuePermission;
import com.futslot.venue.VenueStatus;

/**
 * Creates and changes bookings. Anything that occupies a court first locks the court row
 * ({@link CourtRepository#lockById}), then expires lapsed holds on it, then checks for conflicts, so two
 * requests for the same slot are serialised. The database's exclusion constraint is the final backstop.
 */
@Service
public class BookingService {

	static final int MAX_ACTIVE_HOLDS_PER_PLAYER = 3;

	static final int SEARCH_LIMIT = 30;

	private static final Duration MAX_MANUAL_DURATION = Duration.ofHours(12);

	private static final List<BookingStatus> LIVE = List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED);

	private final BookingRepository bookings;

	private final VenueCustomerRepository customers;

	private final VenueAccess access;

	private final CourtRepository courts;

	private final CourtBlockRepository blocks;

	private final BookedRanges bookedRanges;

	private final AvailabilityService availability;

	private final UserService userService;

	private final UserRepository users;

	private final BookingReferences references;

	private final HoldExpiry holdExpiry;

	private final MembershipService memberships;

	private final DiscountCodeService discountCodes;

	private final AddOnService addOns;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	BookingService(BookingRepository bookings, VenueCustomerRepository customers, VenueAccess access,
			CourtRepository courts, CourtBlockRepository blocks, BookedRanges bookedRanges,
			AvailabilityService availability, UserService userService, UserRepository users,
			BookingReferences references, HoldExpiry holdExpiry, MembershipService memberships,
			DiscountCodeService discountCodes, AddOnService addOns, ApplicationEventPublisher events, Clock clock) {
		this.bookings = bookings;
		this.customers = customers;
		this.access = access;
		this.courts = courts;
		this.blocks = blocks;
		this.bookedRanges = bookedRanges;
		this.availability = availability;
		this.userService = userService;
		this.users = users;
		this.references = references;
		this.holdExpiry = holdExpiry;
		this.memberships = memberships;
		this.discountCodes = discountCodes;
		this.addOns = addOns;
		this.events = events;
		this.clock = clock;
	}

	@Transactional
	public BookingResponse bookAsPlayer(UUID userId, PlayerBookingRequest request) {
		Court court = bookableCourt(request.courtId());
		Venue venue = court.getVenue();
		Instant now = this.clock.instant();
		Instant start = request.startsAt().toInstant();
		lockAndExpireHolds(court, now);

		DatedSlot dated = bookableSlot(venue, court, start);
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
		VenueCustomer customer = customerForPlayer(venue, user);
		Slot slot = dated.slot();
		Booking booking = new Booking(this.references.next(), venue, court, customer, userId, BookingSource.APP,
				start, slot.endsAt().toInstant(), slot.price(), userId, request.notes());
		Benefit benefit = benefit(venue, customer, userId, start, slot.price(), request.membershipId(),
				request.discountCode(), now, true);
		booking.applyDiscount(benefit.discount(), benefit.membershipId(), benefit.discountCodeId());
		booking.setAddOns(this.addOns.resolve(venue.getId(), request.addOns()));
		BigDecimal advance = advanceFor(venue, booking.getTotalAmount());
		if (advance.signum() > 0) {
			booking.holdForPayment(advance, now.plus(Duration.ofMinutes(venue.policy().bookingHoldMinutes())));
		}
		save(booking, customer, now);
		redeem(benefit, booking, userId);
		publish(BookingEventType.CREATED, booking, userId, null);
		return BookingResponse.from(booking);
	}

	/** What booking this slot would cost, with the membership or code applied, without booking it. */
	@Transactional(readOnly = true)
	public QuoteResponse quote(UUID userId, PlayerBookingRequest request) {
		Court court = bookableCourt(request.courtId());
		Venue venue = court.getVenue();
		Instant start = request.startsAt().toInstant();
		Slot slot = bookableSlot(venue, court, start).slot();
		User user = this.userService.get(userId);
		VenueCustomer customer = this.customers.findByVenueIdAndPhone(venue.getId(), user.getPhone()).orElse(null);
		Benefit benefit = benefit(venue, customer, userId, start, slot.price(), request.membershipId(),
				request.discountCode(), this.clock.instant(), false);
		BigDecimal extras = this.addOns.resolve(venue.getId(), request.addOns())
			.entrySet()
			.stream()
			.map(line -> line.getKey().getPrice().multiply(BigDecimal.valueOf(line.getValue())))
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal total = slot.price().add(extras).subtract(benefit.discount());
		return new QuoteResponse(slot.price(), extras, benefit.discount(), total, advanceFor(venue, total),
				benefit.membershipId(), (benefit.code() != null) ? benefit.code().getCode() : null);
	}

	@Transactional
	public BookingResponse bookAsOwner(UUID ownerId, UUID venueId, OwnerBookingRequest request) {
		Venue venue = ownedVenue(ownerId, venueId);
		if (request.source() != BookingSource.WALK_IN && request.source() != BookingSource.PHONE) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SOURCE", "Source must be WALK_IN or PHONE");
		}
		Court court = activeCourt(venueId, request.courtId());
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
		Benefit benefit = benefit(venue, customer, ownerId, start, price, request.membershipId(), null, now, true);
		booking.applyDiscount(benefit.discount(), benefit.membershipId(), null);
		booking.setAddOns(this.addOns.resolve(venueId, request.addOns()));
		save(booking, customer, now);
		redeem(benefit, booking, ownerId);
		publish(BookingEventType.CREATED, booking, ownerId, null);
		return BookingResponse.from(booking);
	}

	@Transactional
	public BookingResponse reschedule(UUID ownerId, UUID venueId, UUID bookingId, RescheduleRequest request) {
		ownedVenue(ownerId, venueId);
		Booking booking = venueBooking(venueId, bookingId);
		requireLive(booking);
		Court court = (request.courtId() != null) ? activeCourt(venueId, request.courtId()) : booking.getCourt();
		Instant now = this.clock.instant();
		Instant start = request.startsAt().toInstant();
		Instant end = (request.endsAt() != null) ? request.endsAt().toInstant()
				: start.plus(Duration.between(booking.getStartsAt(), booking.getEndsAt()));
		validateRange(start, end);
		lockAndExpireHolds(court, now);
		ensureFree(court, start, end, now, booking.getId());
		booking.moveTo(court, start, end);
		flush();
		publish(BookingEventType.RESCHEDULED, booking, ownerId, null);
		return BookingResponse.from(booking);
	}

	/** Staff adding what was taken at the counter (or correcting it), during or after the game. */
	@Transactional
	public BookingResponse setAddOns(UUID userId, UUID venueId, UUID bookingId, AddOnsRequest request) {
		ownedVenue(userId, venueId);
		Booking booking = venueBooking(venueId, bookingId);
		if (!booking.isLive() && booking.getStatus() != BookingStatus.COMPLETED) {
			throw invalidStatus(booking);
		}
		booking.setAddOns(this.addOns.resolve(venueId, request.addOns()));
		return BookingResponse.from(booking);
	}

	@Transactional
	public BookingResponse cancelAsPlayer(UUID userId, UUID bookingId, String reason) {
		Booking booking = this.bookings.findByIdAndCustomerUserId(bookingId, userId)
			.orElseThrow(BookingService::bookingNotFound);
		requireLive(booking);
		Instant now = this.clock.instant();
		if (!now.isBefore(booking.getStartsAt())) {
			throw new ApiException(HttpStatus.CONFLICT, "CANCELLATION_CLOSED",
					"This booking has started. Contact the venue");
		}
		booking.cancel(userId, reason, now);
		publish(BookingEventType.CANCELLED, booking, userId, reason);
		return BookingResponse.from(booking);
	}

	@Transactional
	public BookingResponse cancelAsOwner(UUID ownerId, UUID venueId, UUID bookingId, String reason) {
		ownedVenue(ownerId, venueId);
		Booking booking = venueBooking(venueId, bookingId);
		requireLive(booking);
		booking.cancel(ownerId, reason, this.clock.instant());
		publish(BookingEventType.CANCELLED, booking, ownerId, reason);
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
		return this.bookings.findByIdAndCustomerUserId(bookingId, userId)
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

	/**
	 * Staff looking a booking up by its reference ("FS-7KQ2MX"), the customer's name or a few digits of their
	 * phone, across all dates. At most {@link #SEARCH_LIMIT}, newest first.
	 */
	@Transactional(readOnly = true)
	public List<BookingResponse> searchForVenue(UUID userId, UUID venueId, String query) {
		ownedVenue(userId, venueId);
		String text = (query != null) ? query.strip() : "";
		if (text.length() < 2) {
			return List.of();
		}
		String digits = text.replaceAll("\\D", "");
		String name = "%" + text.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
		// Fewer than 3 digits would match almost everyone; a control character never matches a phone
		String phone = (digits.length() >= 3) ? "%" + digits + "%" : "\u0001";
		return this.bookings.search(venueId, text.toUpperCase(), name, phone, PageRequest.of(0, SEARCH_LIMIT))
			.stream()
			.map(BookingResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public BookingResponse getForOwner(UUID ownerId, UUID venueId, UUID bookingId) {
		ownedVenue(ownerId, venueId);
		return BookingResponse.from(venueBooking(venueId, bookingId));
	}

	// Building blocks shared with RecurringBookingService

	/** Must come first in any write that occupies {@code court}. */
	void lockAndExpireHolds(Court court, Instant now) {
		this.courts.lockById(court.getId());
		this.holdExpiry.expire(court.getId(), now);
	}

	/** Listeners act after commit, so a rolled-back change never notifies anyone. */
	private void publish(BookingEventType type, Booking booking, UUID actorUserId, String reason) {
		this.events.publishEvent(BookingEvent.of(type, booking, actorUserId, reason));
	}

	private Court bookableCourt(UUID courtId) {
		Court court = this.courts.findById(courtId).filter(Court::isActive).orElseThrow(BookingService::courtNotFound);
		if (court.getVenue().getStatus() != VenueStatus.ACTIVE) {
			throw courtNotFound();
		}
		return court;
	}

	/** The slot starting at {@code start}, within the venue's booking window. */
	private DatedSlot bookableSlot(Venue venue, Court court, Instant start) {
		DatedSlot dated = findSlot(venue, court, start)
			.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "SLOT_NOT_FOUND", "That isn't a bookable slot"));
		LocalDate lastBookable = LocalDate.now(this.clock.withZone(zone(venue)))
			.plusDays(venue.policy().maxAdvanceBookingDays());
		if (dated.date().isAfter(lastBookable)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "DATE_OUT_OF_RANGE", "Bookings open up to " + lastBookable);
		}
		return dated;
	}

	/** The venue's advance on {@code total}; nothing when nothing is left to pay. */
	private static BigDecimal advanceFor(Venue venue, BigDecimal total) {
		return total.multiply(BigDecimal.valueOf(venue.policy().advancePercent()))
			.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
	}

	/**
	 * What a membership or discount code takes off {@code price}. With {@code lock} the membership or code
	 * row stays locked until commit; {@link #redeem} then uses it up.
	 */
	private Benefit benefit(Venue venue, VenueCustomer customer, UUID userId, Instant start, BigDecimal price,
			UUID membershipId, String code, Instant now, boolean lock) {
		boolean hasCode = code != null && !code.isBlank();
		if (membershipId != null && hasCode) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "ONE_DISCOUNT_ONLY",
					"Use either a membership or a discount code, not both");
		}
		if (membershipId != null) {
			Membership membership = this.memberships.usable(membershipId, customer,
					start.atZone(zone(venue)).toLocalDate(), lock);
			return new Benefit(MembershipService.discountFor(membership, price), membership, null);
		}
		if (hasCode) {
			DiscountCode discount = this.discountCodes.usable(venue.getId(), code, userId, price, now, lock);
			return new Benefit(DiscountCodeService.discountFor(discount, price), null, discount);
		}
		return new Benefit(BigDecimal.ZERO, null, null);
	}

	private void redeem(Benefit benefit, Booking booking, UUID userId) {
		if (benefit.membership() != null) {
			this.memberships.useGame(benefit.membership());
		}
		if (benefit.code() != null) {
			this.discountCodes.redeem(benefit.code(), booking.getId(), userId, benefit.discount());
		}
	}

	/** The court's slot starting at {@code start}, as players see it now (status included). */
	@Transactional(readOnly = true)
	public Optional<Slot> slotStarting(Court court, Instant start) {
		return findSlot(court.getVenue(), court, start).map(DatedSlot::slot);
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
	BigDecimal priceOfSlots(Venue venue, Court court, Instant start, Instant end) {
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
		conflictAt(court, start, end, now, ignoreBookingId).ifPresent(code -> {
			throw "SLOT_BLOCKED".equals(code)
					? new ApiException(HttpStatus.CONFLICT, code, "The court is blocked at that time") : slotUnavailable();
		});
	}

	/** SLOT_BLOCKED or SLOT_UNAVAILABLE if something already occupies [start, end); call after locking. */
	Optional<String> conflictAt(Court court, Instant start, Instant end, Instant now, UUID ignoreBookingId) {
		if (!this.blocks.findOverlapping(List.of(court.getId()), start, end).isEmpty()) {
			return Optional.of("SLOT_BLOCKED");
		}
		if (!this.bookedRanges.byCourt(List.of(court.getId()), start, end, now, ignoreBookingId).isEmpty()) {
			return Optional.of("SLOT_UNAVAILABLE");
		}
		return Optional.empty();
	}

	void save(Booking booking, VenueCustomer customer, Instant now) {
		customer.recordBooking(now);
		this.bookings.save(booking);
		flush();
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

	/** The venue's customer record for an app user, linked to their account. Call inside a transaction. */
	public VenueCustomer customerForPlayer(Venue venue, User user) {
		VenueCustomer customer = customerFor(venue, user.getPhone(), user.getFullName());
		customer.linkUser(user.getId());
		return customer;
	}

	/** The venue's customer with a phone number staff typed in. Call inside a transaction. */
	public VenueCustomer customerForPhone(Venue venue, String phone, String name) {
		return customerFor(venue, PhoneNumbers.normalize(phone), name.strip());
	}

	/** The venue's customer with this phone, created if new and linked to the app user with that phone. */
	VenueCustomer customerFor(Venue venue, String phone, String name) {
		VenueCustomer customer = this.customers.findByVenueIdAndPhone(venue.getId(), phone)
			.orElseGet(() -> this.customers.save(new VenueCustomer(venue.getId(), phone, name)));
		// A walk-in who also has the app gets the booking in their app
		this.users.findByPhone(phone).ifPresent(user -> customer.linkUser(user.getId()));
		return customer;
	}

	Court activeCourt(UUID venueId, UUID courtId) {
		return this.courts.findByIdAndVenueId(courtId, venueId).filter(Court::isActive).orElseThrow(BookingService::courtNotFound);
	}

	String nextReference() {
		return this.references.next();
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

	/** Owners and staff with MANAGE_BOOKINGS. */
	Venue ownedVenue(UUID userId, UUID venueId) {
		return this.access.require(userId, venueId, VenuePermission.MANAGE_BOOKINGS);
	}

	private Booking venueBooking(UUID venueId, UUID bookingId) {
		return this.bookings.findByIdAndVenueId(bookingId, venueId).orElseThrow(BookingService::bookingNotFound);
	}

	private static void requireLive(Booking booking) {
		if (!booking.isLive()) {
			throw invalidStatus(booking);
		}
	}

	static void validateRange(Instant start, Instant end) {
		if (!end.isAfter(start) || Duration.between(start, end).compareTo(MAX_MANUAL_DURATION) > 0) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE",
					"End must be after start and within 12 hours");
		}
	}

	private static ZoneId zone(Venue venue) {
		return ZoneId.of(venue.getTimezone());
	}

	private record Benefit(BigDecimal discount, Membership membership, DiscountCode code) {

		UUID membershipId() {
			return (this.membership != null) ? this.membership.getId() : null;
		}

		UUID discountCodeId() {
			return (this.code != null) ? this.code.getId() : null;
		}

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
