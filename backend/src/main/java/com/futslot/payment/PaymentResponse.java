package com.futslot.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.futslot.billing.FeaturedListing;
import com.futslot.billing.SubscriptionInvoice;
import com.futslot.booking.Booking;
import com.futslot.membership.Membership;
import com.futslot.tournament.Registration;

/**
 * Only the fields for the payment's purpose are set: booking ones for BOOKING, membership ones for
 * MEMBERSHIP, tournament ones for TOURNAMENT_ENTRY (where {@code customerName} is the team), invoice or
 * listing ones for payments to FutSlot (where it's the venue). A DEPOSIT top-up only names the player, in
 * {@code customerName}.
 */
public record PaymentResponse(UUID id, PaymentPurpose purpose, UUID bookingId, String bookingReference,
		UUID membershipId, String membershipPlanName, UUID tournamentRegistrationId, String tournamentName,
		UUID subscriptionInvoiceId, String invoiceNumber, UUID featuredListingId, String customerName,
		BigDecimal amount, PaymentMethod method, TransactionStatus status, String transactionId, String failureReason,
		Instant paidAt, Instant createdAt) {

	static PaymentResponse from(Payment payment) {
		return from(payment, null);
	}

	/**
	 * {@code venueName} names the payer of an invoice payment, which only knows its venue by id; for every
	 * other payment the name comes from the payment itself.
	 */
	static PaymentResponse from(Payment payment, String venueName) {
		Booking booking = payment.getBooking();
		Membership membership = payment.getMembership();
		Registration entry = payment.getEntry();
		SubscriptionInvoice invoice = payment.getInvoice();
		FeaturedListing listing = payment.getListing();
		String customerName = (booking != null) ? booking.getCustomer().getName()
				: (membership != null) ? membership.getCustomer().getName()
						: (entry != null) ? entry.getTeam().getName()
								: (listing != null) ? listing.getVenue().getName() : venueName;
		return new PaymentResponse(payment.getId(), payment.getPurpose(), (booking != null) ? booking.getId() : null,
				(booking != null) ? booking.getReference() : null, (membership != null) ? membership.getId() : null,
				(membership != null) ? membership.getPlan().getName() : null, (entry != null) ? entry.getId() : null,
				(entry != null) ? entry.getTournament().getName() : null, (invoice != null) ? invoice.getId() : null,
				(invoice != null) ? invoice.getInvoiceNumber() : null, (listing != null) ? listing.getId() : null,
				customerName, payment.getAmount(), payment.getMethod(), payment.getStatus(), payment.getTransactionId(),
				payment.getFailureReason(), payment.getPaidAt(), payment.getCreatedAt());
	}

}
