package com.futslot.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.futslot.booking.Booking;
import com.futslot.membership.Membership;

/** For a BOOKING payment the booking fields are set; for a MEMBERSHIP payment the membership ones. */
public record PaymentResponse(UUID id, PaymentPurpose purpose, UUID bookingId, String bookingReference,
		UUID membershipId, String membershipPlanName, String customerName, BigDecimal amount, PaymentMethod method,
		TransactionStatus status, String transactionId, String failureReason, Instant paidAt, Instant createdAt) {

	static PaymentResponse from(Payment payment) {
		Booking booking = payment.getBooking();
		Membership membership = payment.getMembership();
		String customerName = (booking != null) ? booking.getCustomer().getName() : membership.getCustomer().getName();
		return new PaymentResponse(payment.getId(), payment.getPurpose(), (booking != null) ? booking.getId() : null,
				(booking != null) ? booking.getReference() : null, (membership != null) ? membership.getId() : null,
				(membership != null) ? membership.getPlan().getName() : null, customerName, payment.getAmount(),
				payment.getMethod(), payment.getStatus(), payment.getTransactionId(), payment.getFailureReason(),
				payment.getPaidAt(), payment.getCreatedAt());
	}

}
