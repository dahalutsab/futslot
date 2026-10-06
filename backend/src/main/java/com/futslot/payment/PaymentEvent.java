package com.futslot.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.futslot.booking.Booking;
import com.futslot.booking.BookingResponse;
import com.futslot.booking.BookingStatus;

/** Published after payment activity on a booking; consumed by notifications after commit. */
public record PaymentEvent(Type type, UUID bookingId, String reference, BookingStatus bookingStatus, UUID venueId,
		String venueName,
		String customerName, UUID playerUserId, OffsetDateTime startsAt, BigDecimal amount, PaymentMethod method,
		String reason, Instant holdExpiresAt, UUID actorUserId) {

	public enum Type {

		/** Player submitted a payment for staff to verify. */
		SUBMITTED,
		/** Staff confirmed the money arrived (or recorded it directly). */
		RECEIVED,
		/** Staff couldn't find the payment. */
		REJECTED,
		/** A cancellation left the venue owing money back. */
		REFUND_DUE,
		/** The venue paid the refund. */
		REFUNDED

	}

	static PaymentEvent of(Type type, Booking booking, BigDecimal amount, PaymentMethod method, String reason,
			UUID actorUserId) {
		return new PaymentEvent(type, booking.getId(), booking.getReference(), booking.getStatus(), booking.getVenue().getId(),
				booking.getVenue().getName(), booking.getCustomer().getName(), booking.getCustomer().getUserId(),
				BookingResponse.from(booking).startsAt(), amount, method, reason, booking.getHoldExpiresAt(),
				actorUserId);
	}

}
