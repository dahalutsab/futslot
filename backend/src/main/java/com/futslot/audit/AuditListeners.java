package com.futslot.audit;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.futslot.booking.BookingEvent;
import com.futslot.payment.MembershipPaymentEvent;
import com.futslot.payment.PaymentEvent;
import com.futslot.venue.StaffAdded;
import com.futslot.venue.VenueModerated;

/**
 * What goes in the audit log: things that matter later in a dispute. Money taken, approved, turned down
 * or paid back; bookings entered by the venue, moved, or cancelled (by anyone); staff given access; venues
 * approved, rejected or suspended. Players' own app bookings aren't logged. Runs inside the change's own
 * transaction.
 */
@Component
class AuditListeners {

	private final AuditTrail trail;

	AuditListeners(AuditTrail trail) {
		this.trail = trail;
	}

	@EventListener
	void on(BookingEvent event) {
		boolean byPlayer = event.actorUserId() != null && event.actorUserId().equals(event.playerUserId());
		switch (event.type()) {
			case CREATED -> {
				if (!event.source().byPlayer()) {
					this.trail.record(event.actorUserId(), event.venueId(), "BOOKING_CREATED", "BOOKING", event.bookingId(),
							"reference", event.reference(), "source", event.source(), "customer", event.customerName(),
							"startsAt", event.startsAt(), "total", event.totalAmount());
				}
			}
			case CANCELLED -> this.trail.record(event.actorUserId(), event.venueId(),
					byPlayer ? "BOOKING_CANCELLED_BY_PLAYER" : "BOOKING_CANCELLED", "BOOKING", event.bookingId(),
					"reference", event.reference(), "customer", event.customerName(), "startsAt", event.startsAt(),
					"reason", event.reason());
			case RESCHEDULED -> this.trail.record(event.actorUserId(), event.venueId(), "BOOKING_RESCHEDULED", "BOOKING",
					event.bookingId(), "reference", event.reference(), "court", event.courtName(), "startsAt",
					event.startsAt());
			case NO_SHOW -> this.trail.record(event.actorUserId(), event.venueId(), "BOOKING_NO_SHOW", "BOOKING",
					event.bookingId(), "reference", event.reference(), "customer", event.customerName(), "startsAt",
					event.startsAt());
			case EXPIRED -> {
			}
		}
	}

	@EventListener
	void on(PaymentEvent event) {
		String action = switch (event.type()) {
			case RECEIVED -> "PAYMENT_RECEIVED";
			case REJECTED -> "PAYMENT_REJECTED";
			case REFUNDED -> "REFUND_PAID";
			case SUBMITTED, REFUND_DUE -> null;
		};
		if (action != null) {
			this.trail.record(event.actorUserId(), event.venueId(), action, "BOOKING", event.bookingId(), "reference",
					event.reference(), "customer", event.customerName(), "amount", event.amount(), "method",
					event.method(), "reason", event.reason());
		}
	}

	@EventListener
	void on(MembershipPaymentEvent event) {
		if (event.type() == PaymentEvent.Type.RECEIVED || event.type() == PaymentEvent.Type.REJECTED) {
			this.trail.record(event.actorUserId(), event.venueId(),
					(event.type() == PaymentEvent.Type.RECEIVED) ? "MEMBERSHIP_PAYMENT_RECEIVED" : "MEMBERSHIP_PAYMENT_REJECTED",
					"MEMBERSHIP", event.membershipId(), "plan", event.planName(), "customer", event.customerName(),
					"amount", event.amount(), "method", event.method(), "reason", event.reason());
		}
	}

	@EventListener
	void on(StaffAdded event) {
		this.trail.record(event.addedBy(), event.venueId(), "STAFF_ADDED", "USER", event.userId(), "role", event.role());
	}

	@EventListener
	void on(VenueModerated event) {
		this.trail.record(event.adminId(), event.venueId(), "VENUE_" + event.status(), "VENUE", event.venueId(), "reason",
				event.reason());
	}

}
