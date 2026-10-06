package com.futslot.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Published after a booking changes. Carries everything listeners need so they don't have to load the
 * booking again. {@code playerUserId} is the app user behind the booking's customer, if any;
 * {@code actorUserId} is who made the change (null for system changes such as hold expiry).
 */
public record BookingEvent(BookingEventType type, UUID bookingId, String reference, BookingStatus status,
		BookingSource source, UUID venueId, String venueName, UUID venueOwnerId, String courtName,
		String customerName, UUID playerUserId, OffsetDateTime startsAt, BigDecimal totalAmount,
		BigDecimal advanceRequired, Instant holdExpiresAt, UUID actorUserId, String reason) {

	static BookingEvent of(BookingEventType type, Booking booking, UUID actorUserId, String reason) {
		return new BookingEvent(type, booking.getId(), booking.getReference(), booking.getStatus(),
				booking.getSource(), booking.getVenue().getId(), booking.getVenue().getName(),
				booking.getVenue().getOwnerId(), booking.getCourt().getName(), booking.getCustomer().getName(),
				booking.getCustomer().getUserId(), BookingResponse.from(booking).startsAt(), booking.getTotalAmount(),
				booking.getAdvanceRequired(), booking.getHoldExpiresAt(), actorUserId, reason);
	}

}
