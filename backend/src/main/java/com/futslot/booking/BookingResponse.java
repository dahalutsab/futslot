package com.futslot.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/** Start and end are in the venue's local offset. */
public record BookingResponse(UUID id, String reference, VenueRef venue, CourtRef court, CustomerRef customer,
		BookingSource source, BookingStatus status, OffsetDateTime startsAt, OffsetDateTime endsAt,
		BigDecimal priceAmount, BigDecimal discountAmount, UUID membershipId, UUID discountCodeId,
		BigDecimal totalAmount, BigDecimal advanceRequired, BigDecimal paidAmount, PaymentStatus paymentStatus,
		Instant holdExpiresAt, String notes, Instant cancelledAt, String cancellationReason, Instant createdAt) {

	public static BookingResponse from(Booking booking) {
		ZoneId zone = ZoneId.of(booking.getVenue().getTimezone());
		return new BookingResponse(booking.getId(), booking.getReference(),
				new VenueRef(booking.getVenue().getId(), booking.getVenue().getName(), booking.getVenue().getSlug()),
				new CourtRef(booking.getCourt().getId(), booking.getCourt().getName()),
				new CustomerRef(booking.getCustomer().getId(), booking.getCustomer().getName(),
						booking.getCustomer().getPhone()),
				booking.getSource(), booking.getStatus(), booking.getStartsAt().atZone(zone).toOffsetDateTime(),
				booking.getEndsAt().atZone(zone).toOffsetDateTime(), booking.getPriceAmount(),
				booking.getDiscountAmount(), booking.getMembershipId(), booking.getDiscountCodeId(),
				booking.getTotalAmount(), booking.getAdvanceRequired(), booking.getPaidAmount(),
				booking.getPaymentStatus(), booking.getHoldExpiresAt(), booking.getNotes(), booking.getCancelledAt(),
				booking.getCancellationReason(), booking.getCreatedAt());
	}

	public record VenueRef(UUID id, String name, String slug) {

	}

	public record CourtRef(UUID id, String name) {

	}

	public record CustomerRef(UUID id, String name, String phone) {

	}

}
