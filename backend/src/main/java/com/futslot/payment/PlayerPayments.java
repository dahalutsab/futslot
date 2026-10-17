package com.futslot.payment;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A player reporting a payment from outside this package: a booking's advance paid to the venue (from places other
 * than the app API), or a top-up sent to FutSlot for their booking deposit.
 */
@Service
public class PlayerPayments {

	private final PaymentService payments;

	private final PaymentRepository repository;

	private final PlatformPaymentService platformPayments;

	PlayerPayments(PaymentService payments, PaymentRepository repository, PlatformPaymentService platformPayments) {
		this.payments = payments;
		this.repository = repository;
		this.platformPayments = platformPayments;
	}

	/** "I paid Rs X on eSewa, transaction Y": holds the slot while the venue checks. */
	public void claimForBooking(UUID userId, UUID bookingId, PaymentMethod method, String transactionId, BigDecimal amount) {
		this.payments.claim(userId, bookingId, new PaymentRequests.Claim(method, transactionId, amount));
	}

	@Transactional(readOnly = true)
	public boolean awaitingCheck(UUID bookingId) {
		return this.repository.existsByBookingIdAndStatus(bookingId, TransactionStatus.PENDING);
	}

	/** "I sent FutSlot Rs X on eSewa, transaction Y" to fill my deposit; it goes in once an admin finds it. */
	public PaymentResponse claimDepositTopUp(UUID userId, PaymentMethod method, String transactionId, BigDecimal amount) {
		return this.platformPayments.claimDeposit(userId, new PaymentRequests.Claim(method, transactionId, amount));
	}

	@Transactional(readOnly = true)
	public boolean depositTopUpBeingChecked(UUID userId) {
		return this.repository.existsByPurposeAndPayerUserIdAndStatus(PaymentPurpose.DEPOSIT, userId,
				TransactionStatus.PENDING);
	}

}
