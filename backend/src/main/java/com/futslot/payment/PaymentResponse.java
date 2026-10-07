package com.futslot.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.futslot.booking.Booking;
import com.futslot.membership.Membership;
import com.futslot.tournament.Registration;

/**
 * Only the fields for the payment's purpose are set: booking ones for BOOKING, membership ones for
 * MEMBERSHIP, tournament ones for TOURNAMENT_ENTRY (where {@code customerName} is the team).
 */
public record PaymentResponse(UUID id, PaymentPurpose purpose, UUID bookingId, String bookingReference,
		UUID membershipId, String membershipPlanName, UUID tournamentRegistrationId, String tournamentName,
		String customerName, BigDecimal amount, PaymentMethod method, TransactionStatus status, String transactionId,
		String failureReason, Instant paidAt, Instant createdAt) {

	static PaymentResponse from(Payment payment) {
		Booking booking = payment.getBooking();
		Membership membership = payment.getMembership();
		Registration entry = payment.getEntry();
		String customerName = (booking != null) ? booking.getCustomer().getName()
				: (membership != null) ? membership.getCustomer().getName() : entry.getTeam().getName();
		return new PaymentResponse(payment.getId(), payment.getPurpose(), (booking != null) ? booking.getId() : null,
				(booking != null) ? booking.getReference() : null, (membership != null) ? membership.getId() : null,
				(membership != null) ? membership.getPlan().getName() : null, (entry != null) ? entry.getId() : null,
				(entry != null) ? entry.getTournament().getName() : null, customerName, payment.getAmount(),
				payment.getMethod(), payment.getStatus(), payment.getTransactionId(), payment.getFailureReason(),
				payment.getPaidAt(), payment.getCreatedAt());
	}

}
