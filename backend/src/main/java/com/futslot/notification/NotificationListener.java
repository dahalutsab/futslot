package com.futslot.notification;

import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import com.futslot.booking.BookingEvent;
import com.futslot.booking.BookingSource;
import com.futslot.booking.BookingStatus;
import com.futslot.venue.VenueModerated;

/**
 * Turns domain events into notifications once the change has committed. Runs in its own transaction
 * because the originating one is already finished at that point.
 */
@Component
class NotificationListener {

	private final NotificationService notifications;

	NotificationListener(NotificationService notifications) {
		this.notifications = notifications;
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(BookingEvent event) {
		String slot = event.courtName() + ", " + Messages.when(event.startsAt());
		Map<String, String> data = Map.of("bookingId", event.bookingId().toString(), "venueId", event.venueId().toString());
		switch (event.type()) {
			case CREATED -> created(event, slot, data);
			case CANCELLED -> cancelled(event, slot, data);
			case RESCHEDULED -> toPlayer(event, NotificationType.BOOKING_RESCHEDULED, "Your booking was moved",
					event.venueName() + " moved your booking to " + slot + ".", data);
			case EXPIRED -> toPlayer(event, NotificationType.BOOKING_EXPIRED, "Booking hold expired",
					"Payment wasn't completed in time, so your hold on " + event.venueName() + ", " + slot
							+ " was released.", data);
		}
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(VenueModerated event) {
		Map<String, String> data = Map.of("venueId", event.venueId().toString());
		switch (event.status()) {
			case ACTIVE -> this.notifications.send(event.ownerId(), NotificationType.VENUE_APPROVED, "Your venue is live",
					event.venueName() + " is approved and visible to players.", data);
			case REJECTED -> this.notifications.send(event.ownerId(), NotificationType.VENUE_REJECTED,
					"Venue not approved", event.venueName() + ": " + event.reason(), data);
			case SUSPENDED -> this.notifications.send(event.ownerId(), NotificationType.VENUE_SUSPENDED,
					"Venue suspended", event.venueName() + " is hidden from players: " + event.reason(), data);
			default -> {
			}
		}
	}

	private void created(BookingEvent event, String slot, Map<String, String> data) {
		if (event.source() == BookingSource.APP) {
			if (event.status() == BookingStatus.PENDING_PAYMENT) {
				String deadline = Messages.time(event.holdExpiresAt().atZone(event.startsAt().getOffset()).toOffsetDateTime());
				toPlayer(event, NotificationType.BOOKING_PAYMENT_DUE, "Complete your payment",
						"Pay " + Messages.rupees(event.advanceRequired()) + " by " + deadline + " to confirm "
								+ event.venueName() + ", " + slot + ".", data);
			}
			else {
				toPlayer(event, NotificationType.BOOKING_CONFIRMED, "Booking confirmed",
						event.venueName() + ", " + slot + ". Ref " + event.reference() + ".", data);
			}
			toOwner(event, NotificationType.NEW_BOOKING, "New booking", event.customerName() + " booked " + slot + ".",
					data);
		}
		else {
			toPlayer(event, NotificationType.BOOKING_CREATED_BY_VENUE, event.venueName() + " booked a slot for you",
					slot + ". Ref " + event.reference() + ".", data);
		}
	}

	private void cancelled(BookingEvent event, String slot, Map<String, String> data) {
		String reason = (event.reason() != null && !event.reason().isBlank()) ? " Reason: " + event.reason() : "";
		if (Objects.equals(event.actorUserId(), event.playerUserId())) {
			toOwner(event, NotificationType.BOOKING_CANCELLED, "Booking cancelled",
					event.customerName() + " cancelled " + slot + "." + reason, data);
		}
		else {
			toPlayer(event, NotificationType.BOOKING_CANCELLED, event.venueName() + " cancelled your booking",
					slot + "." + reason, data);
		}
	}

	private void toPlayer(BookingEvent event, NotificationType type, String title, String body, Map<String, String> data) {
		if (event.playerUserId() != null) {
			this.notifications.send(event.playerUserId(), type, title, body, data);
		}
	}

	private void toOwner(BookingEvent event, NotificationType type, String title, String body, Map<String, String> data) {
		// Owners don't need to hear about changes they made themselves
		if (!event.venueOwnerId().equals(event.actorUserId())) {
			this.notifications.send(event.venueOwnerId(), type, title, body, data);
		}
	}

}
