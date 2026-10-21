package com.futslot.waitlist;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import com.futslot.booking.Booking;
import com.futslot.booking.BookingEvent;
import com.futslot.booking.BookingEventType;
import com.futslot.booking.BookingLookup;
import com.futslot.booking.BookingService;
import com.futslot.common.ApiException;
import com.futslot.slot.Slot;
import com.futslot.slot.SlotStatus;
import com.futslot.venue.Court;
import com.futslot.venue.CourtRepository;
import com.futslot.venue.VenueStatus;

/**
 * Waiting for a booked slot. When a booking on it is cancelled or its hold lapses, everyone still waiting is
 * told at once, and whoever books first gets it; the others stay listed in case it frees again.
 */
@Service
class WaitlistService {

	static final int MAX_OPEN = 5;

	private final WaitlistRepository entries;

	private final CourtRepository courts;

	private final BookingService bookingService;

	private final BookingLookup bookings;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	WaitlistService(WaitlistRepository entries, CourtRepository courts, BookingService bookingService,
			BookingLookup bookings, ApplicationEventPublisher events, Clock clock) {
		this.entries = entries;
		this.courts = courts;
		this.bookingService = bookingService;
		this.bookings = bookings;
		this.events = events;
		this.clock = clock;
	}

	@Transactional
	WaitlistView join(UUID userId, WaitlistRequest request) {
		Court court = this.courts.findById(request.courtId())
			.filter(Court::isActive)
			.filter(found -> found.getVenue().getStatus() == VenueStatus.ACTIVE)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COURT_NOT_FOUND", "Court not found"));
		Instant start = request.startsAt().toInstant();
		Slot slot = this.bookingService.slotStarting(court, start)
			.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "SLOT_NOT_FOUND", "That isn't a bookable slot"));
		if (slot.status() == SlotStatus.AVAILABLE) {
			throw new ApiException(HttpStatus.CONFLICT, "SLOT_AVAILABLE", "That slot is free. Book it now");
		}
		if (slot.status() != SlotStatus.BOOKED) {
			throw new ApiException(HttpStatus.CONFLICT, "SLOT_NOT_WAITABLE", "You can only wait for a booked slot");
		}
		if (this.bookings.playerHolds(userId, court.getId(), start, slot.endsAt().toInstant())) {
			throw new ApiException(HttpStatus.CONFLICT, "SLOT_IS_YOURS", "You've booked this slot already");
		}
		WaitlistEntry entry = this.entries.findByCourtIdAndUserIdAndStartsAt(court.getId(), userId, start).orElse(null);
		Instant now = this.clock.instant();
		if (entry != null && entry.isOpenAt(now)) {
			throw new ApiException(HttpStatus.CONFLICT, "ALREADY_WAITING", "You're already on the waitlist for this slot");
		}
		if (this.entries.countOpen(userId, now) >= MAX_OPEN) {
			throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_WAITLISTS",
					"You can wait for " + MAX_OPEN + " slots at a time", MAX_OPEN);
		}
		if (entry == null) {
			entry = this.entries.save(new WaitlistEntry(court, userId, start, slot.endsAt().toInstant()));
		}
		else {
			entry.rejoin();
		}
		return view(entry, now);
	}

	@Transactional(readOnly = true)
	List<WaitlistView> mine(UUID userId) {
		Instant now = this.clock.instant();
		return this.entries.findByUserIdOrderByStartsAtDesc(userId).stream().map(entry -> view(entry, now)).toList();
	}

	@Transactional
	void leave(UUID userId, UUID entryId) {
		WaitlistEntry entry = this.entries.findByIdAndUserId(entryId, userId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WAITLIST_NOT_FOUND", "Not on that waitlist"));
		entry.setStatus(WaitlistStatus.CANCELLED);
	}

	/**
	 * After a booking commits: a new booking marks the booker's own entry as BOOKED; a cancelled or lapsed
	 * one frees its time, so whoever waits for it hears (if nothing else has taken it).
	 */
	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(BookingEvent event) {
		Booking booking = this.bookings.byId(event.bookingId());
		Instant now = this.clock.instant();
		List<WaitlistEntry> waiting = this.entries.findOpenOverlapping(booking.getCourt().getId(), booking.getStartsAt(),
				booking.getEndsAt(), now);
		if (waiting.isEmpty()) {
			return;
		}
		if (event.type() == BookingEventType.CREATED) {
			waiting.stream()
				.filter(entry -> entry.getUserId().equals(event.playerUserId()))
				.forEach(entry -> entry.setStatus(WaitlistStatus.BOOKED));
			return;
		}
		if (event.type() != BookingEventType.CANCELLED && event.type() != BookingEventType.EXPIRED) {
			return;
		}
		Court court = booking.getCourt();
		ZoneId zone = ZoneId.of(court.getVenue().getTimezone());
		for (WaitlistEntry entry : waiting) {
			boolean free = this.bookingService.slotStarting(court, entry.getStartsAt())
				.filter(slot -> slot.status() == SlotStatus.AVAILABLE)
				.isPresent();
			if (free) {
				entry.notified(now);
			}
		}
		// Group by slot: each freed slot is one message to everyone waiting for it
		waiting.stream()
			.filter(entry -> entry.getStatus() == WaitlistStatus.NOTIFIED && now.equals(entry.getNotifiedAt()))
			.map(WaitlistEntry::getStartsAt)
			.distinct()
			.forEach(start -> {
				List<UUID> recipients = waiting.stream()
					.filter(entry -> entry.getStartsAt().equals(start) && now.equals(entry.getNotifiedAt()))
					.map(WaitlistEntry::getUserId)
					.toList();
				this.events.publishEvent(new SlotFreed(court.getVenue().getId(), court.getVenue().getName(),
						court.getVenue().getSlug(), court.getId(), court.getName(), start.atZone(zone).toOffsetDateTime(),
						recipients));
			});
	}

	private static WaitlistView view(WaitlistEntry entry, Instant now) {
		Court court = entry.getCourt();
		ZoneId zone = ZoneId.of(court.getVenue().getTimezone());
		OffsetDateTime start = entry.getStartsAt().atZone(zone).toOffsetDateTime();
		OffsetDateTime end = entry.getEndsAt().atZone(zone).toOffsetDateTime();
		return new WaitlistView(entry.getId(), court.getVenue().getId(), court.getVenue().getName(),
				court.getVenue().getSlug(), court.getId(), court.getName(), start, end, entry.statusAt(now),
				entry.getNotifiedAt(), entry.getCreatedAt());
	}

}
