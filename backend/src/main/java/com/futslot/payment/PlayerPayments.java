package com.futslot.payment;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A player telling the venue they've paid a booking's advance, from places other than the app API. */
@Service
public class PlayerPayments {

	private final PaymentService payments;

	private final PaymentRepository repository;

	PlayerPayments(PaymentService payments, PaymentRepository repository) {
		this.payments = payments;
		this.repository = repository;
	}

	/** "I paid Rs X on eSewa, transaction Y": holds the slot while the venue checks. */
	public void claimForBooking(UUID userId, UUID bookingId, PaymentMethod method, String transactionId, BigDecimal amount) {
		this.payments.claim(userId, bookingId, new PaymentRequests.Claim(method, transactionId, amount));
	}

	@Transactional(readOnly = true)
	public boolean awaitingCheck(UUID bookingId) {
		return this.repository.existsByBookingIdAndStatus(bookingId, TransactionStatus.PENDING);
	}

}
