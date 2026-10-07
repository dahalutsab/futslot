package com.futslot.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.futslot.booking.Booking;

/** Includes who to pay back and how they originally paid, so staff can return the money the same way. */
public record RefundResponse(UUID id, UUID bookingId, String bookingReference, String customerName,
		String customerPhone, BigDecimal amount, PaymentMethod originalMethod, String reason, RefundStatus status,
		String reference, Instant createdAt, Instant processedAt) {

	static RefundResponse from(Refund refund) {
		Booking booking = refund.getPayment().getBooking();
		return new RefundResponse(refund.getId(), booking.getId(), booking.getReference(),
				booking.getCustomer().getName(), booking.getCustomer().getPhone(), refund.getAmount(),
				refund.getPayment().getMethod(), refund.getReason(), refund.getStatus(), refund.getReference(),
				refund.getCreatedAt(), refund.getProcessedAt());
	}

}
