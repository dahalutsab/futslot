package com.futslot.booking;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.ApiException;
import com.futslot.common.PhoneNumbers;
import com.futslot.venue.Court;
import com.futslot.venue.Venue;

@Service
class RecurringBookingService {

	private static final List<BookingStatus> LIVE = List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED);

	private final RecurringBookingRepository series;

	private final BookingRepository bookings;

	private final BookingService bookingService;

	private final ApplicationEventPublisher events;

	private final int horizonWeeks;

	private final Clock clock;

	RecurringBookingService(RecurringBookingRepository series, BookingRepository bookings,
			BookingService bookingService, ApplicationEventPublisher events,
			@Value("${futslot.bookings.recurring.horizon-weeks:8}") int horizonWeeks, Clock clock) {
		this.series = series;
		this.bookings = bookings;
		this.bookingService = bookingService;
		this.events = events;
		this.horizonWeeks = horizonWeeks;
		this.clock = clock;
	}

	@Transactional
	RecurringBookingResponse create(UUID userId, UUID venueId, RecurringBookingRequest request) {
		Venue venue = this.bookingService.ownedVenue(userId, venueId);
		Court court = this.bookingService.activeCourt(venueId, request.courtId());
		LocalTime end = (request.endTime() != null) ? request.endTime()
				: request.startTime().plusMinutes(court.getSlotDurationMinutes());
		if (!end.isAfter(request.startTime())) {
			throw badRequest("INVALID_TIME_RANGE", "End time must be after start time (weekly slots can't cross midnight)");
		}
		LocalDate today = today(venue);
		LocalDate first = request.startsOn().with(TemporalAdjusters.nextOrSame(request.dayOfWeek()));
		if (request.startsOn().isBefore(today)) {
			throw badRequest("INVALID_DATE_RANGE", "Start date can't be in the past");
		}
		if (request.endsOn() != null && request.endsOn().isBefore(first)) {
			throw badRequest("INVALID_DATE_RANGE", "The series ends before its first " + request.dayOfWeek());
		}
		BigDecimal price = (request.price() != null) ? request.price()
				: this.bookingService.priceOfSlots(venue, court, instant(venue, first, request.startTime()),
						instant(venue, first, end));
		VenueCustomer customer = this.bookingService.customerFor(venue, PhoneNumbers.normalize(request.customerPhone()),
				request.customerName().strip());

		RecurringBooking created = this.series.save(new RecurringBooking(venue, court, customer, request.dayOfWeek(),
				request.startTime(), end, first, request.endsOn(), price, userId));
		List<RecurringBookingResponse.Skipped> skipped = generate(created, userId);
		this.events.publishEvent(RecurringBookingEvent.of(RecurringBookingEvent.Type.CREATED, created,
				skipped.stream().map(RecurringBookingResponse.Skipped::date).toList(), userId));
		return response(created, skipped);
	}

	@Transactional(readOnly = true)
	List<RecurringBookingResponse> list(UUID userId, UUID venueId) {
		this.bookingService.ownedVenue(userId, venueId);
		return this.series.findByVenueIdAndStatusNotOrderByDayOfWeekAscStartTimeAsc(venueId, RecurringStatus.CANCELLED)
			.stream()
			.map(found -> response(found, List.of()))
			.toList();
	}

	@Transactional(readOnly = true)
	RecurringBookingResponse get(UUID userId, UUID venueId, UUID seriesId) {
		this.bookingService.ownedVenue(userId, venueId);
		return response(find(venueId, seriesId), List.of());
	}

	/** Frees every future week; the series keeps its place in the list and can be resumed. */
	@Transactional
	RecurringBookingResponse pause(UUID userId, UUID venueId, UUID seriesId) {
		this.bookingService.ownedVenue(userId, venueId);
		RecurringBooking found = find(venueId, seriesId);
		requireStatus(found, RecurringStatus.ACTIVE);
		cancelFutureOccurrences(found, userId, "Weekly booking paused");
		found.pause();
		return response(found, List.of());
	}

	@Transactional
	RecurringBookingResponse resume(UUID userId, UUID venueId, UUID seriesId) {
		this.bookingService.ownedVenue(userId, venueId);
		RecurringBooking found = find(venueId, seriesId);
		requireStatus(found, RecurringStatus.PAUSED);
		found.resume();
		return response(found, generate(found, userId));
	}

	@Transactional
	RecurringBookingResponse cancel(UUID userId, UUID venueId, UUID seriesId) {
		this.bookingService.ownedVenue(userId, venueId);
		RecurringBooking found = find(venueId, seriesId);
		if (found.getStatus() == RecurringStatus.CANCELLED) {
			throw invalidStatus(found);
		}
		cancelFutureOccurrences(found, userId, "Weekly booking cancelled");
		found.cancel();
		this.events.publishEvent(RecurringBookingEvent.of(RecurringBookingEvent.Type.CANCELLED, found, List.of(), userId));
		return response(found, List.of());
	}

	/** Called by the scheduler to keep active series booked {@code horizonWeeks} ahead. */
	@Transactional
	void extend(UUID seriesId) {
		RecurringBooking found = this.series.findById(seriesId).orElseThrow();
		List<RecurringBookingResponse.Skipped> skipped = generate(found, null);
		if (!skipped.isEmpty()) {
			this.events.publishEvent(RecurringBookingEvent.of(RecurringBookingEvent.Type.SKIPPED, found,
					skipped.stream().map(RecurringBookingResponse.Skipped::date).toList(), null));
		}
	}

	List<UUID> needingGeneration() {
		return this.series.findNeedingGeneration(LocalDate.now(this.clock).plusWeeks(this.horizonWeeks));
	}

	/**
	 * Books every week from the last generated date (or today) up to the horizon, skipping weeks
	 * already in the past and weeks where the court is taken or blocked.
	 */
	private List<RecurringBookingResponse.Skipped> generate(RecurringBooking found, UUID actorUserId) {
		Venue venue = found.getVenue();
		Court court = found.getCourt();
		Instant now = this.clock.instant();
		LocalDate today = today(venue);
		LocalDate horizon = today.plusWeeks(this.horizonWeeks);
		LocalDate until = (found.getEndsOn() != null && found.getEndsOn().isBefore(horizon)) ? found.getEndsOn() : horizon;
		LocalDate from = (found.getGeneratedUntil() != null) ? found.getGeneratedUntil().plusDays(1) : found.getStartsOn();
		if (from.isBefore(today)) {
			from = today;
		}
		from = from.with(TemporalAdjusters.nextOrSame(found.getDayOfWeek()));

		List<RecurringBookingResponse.Skipped> skipped = new ArrayList<>();
		if (!from.isAfter(until)) {
			this.bookingService.lockAndExpireHolds(court, now);
		}
		for (LocalDate date = from; !date.isAfter(until); date = date.plusWeeks(1)) {
			Instant start = instant(venue, date, found.getStartTime());
			Instant end = instant(venue, date, found.getEndTime());
			if (!start.isAfter(now)) {
				continue;
			}
			Optional<String> conflict = this.bookingService.conflictAt(court, start, end, now, null);
			if (conflict.isPresent()) {
				skipped.add(new RecurringBookingResponse.Skipped(date, conflict.get()));
				continue;
			}
			Booking occurrence = Booking.occurrence(this.bookingService.nextReference(), found, start, end,
					(actorUserId != null) ? actorUserId : found.getCustomer().getUserId());
			this.bookingService.save(occurrence, found.getCustomer(), now);
		}
		found.generatedUntil(until);
		return skipped;
	}

	private void cancelFutureOccurrences(RecurringBooking found, UUID userId, String reason) {
		Instant now = this.clock.instant();
		this.bookings.findOccurrencesAfter(found.getId(), now, LIVE).forEach(booking -> booking.cancel(userId, reason, now));
	}

	private RecurringBookingResponse response(RecurringBooking found, List<RecurringBookingResponse.Skipped> skipped) {
		List<BookingResponse> upcoming = this.bookings.findOccurrencesAfter(found.getId(), this.clock.instant(), LIVE)
			.stream()
			.map(BookingResponse::from)
			.toList();
		VenueCustomer customer = found.getCustomer();
		return new RecurringBookingResponse(found.getId(),
				new BookingResponse.CourtRef(found.getCourt().getId(), found.getCourt().getName()),
				new BookingResponse.CustomerRef(customer.getId(), customer.getName(), customer.getPhone()),
				found.getDayOfWeek(), found.getStartTime(), found.getEndTime(), found.getStartsOn(), found.getEndsOn(),
				found.getPrice(), found.getStatus(), found.getGeneratedUntil(), upcoming, skipped);
	}

	private RecurringBooking find(UUID venueId, UUID seriesId) {
		return this.series.findByIdAndVenueId(seriesId, venueId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RECURRING_BOOKING_NOT_FOUND",
					"Weekly booking not found"));
	}

	private static void requireStatus(RecurringBooking found, RecurringStatus expected) {
		if (found.getStatus() != expected) {
			throw invalidStatus(found);
		}
	}

	private static ApiException invalidStatus(RecurringBooking found) {
		return new ApiException(HttpStatus.CONFLICT, "INVALID_RECURRING_STATUS",
				"Not possible for a weekly booking that is " + found.getStatus());
	}

	private LocalDate today(Venue venue) {
		return LocalDate.now(this.clock.withZone(ZoneId.of(venue.getTimezone())));
	}

	private static Instant instant(Venue venue, LocalDate date, LocalTime time) {
		return date.atTime(time).atZone(ZoneId.of(venue.getTimezone())).toInstant();
	}

	private static ApiException badRequest(String code, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, code, message);
	}

}
