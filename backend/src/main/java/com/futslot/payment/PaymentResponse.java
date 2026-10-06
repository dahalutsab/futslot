package com.futslot.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID id, UUID bookingId, String bookingReference, String customerName,
		BigDecimal amount, PaymentMethod method, TransactionStatus status, String transactionId,
		String failureReason, Instant paidAt, Instant createdAt) {

	static PaymentResponse from(Payment payment) {
		return new PaymentResponse(payment.getId(), payment.getBooking().getId(), payment.getBooking().getReference(),
				payment.getBooking().getCustomer().getName(), payment.getAmount(), payment.getMethod(),
				payment.getStatus(), payment.getTransactionId(), payment.getFailureReason(), payment.getPaidAt(),
				payment.getCreatedAt());
	}

}
