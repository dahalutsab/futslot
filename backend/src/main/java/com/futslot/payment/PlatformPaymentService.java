package com.futslot.payment;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.billing.FeaturedListing;
import com.futslot.billing.FeaturedService;
import com.futslot.billing.SubscriptionInvoice;
import com.futslot.billing.SubscriptionService;
import com.futslot.common.ApiException;
import com.futslot.deposit.DepositService;
import com.futslot.user.UserService;

/**
 * Payments to FutSlot: venues paying subscription invoices and featured listings, and players filling their
 * booking deposit. Until a gateway is connected the payer pays FutSlot's own wallet or account and reports the
 * transaction; a FutSlot admin verifies it, or records a payment taken another way. These payments have no
 * venue_id, so they never show up as a venue's income or in its payment queue.
 */
@Service
class PlatformPaymentService {

	private static final EnumSet<PaymentPurpose> PLATFORM = EnumSet.of(PaymentPurpose.SUBSCRIPTION,
			PaymentPurpose.FEATURED_LISTING, PaymentPurpose.DEPOSIT);

	private final PaymentRepository payments;

	private final PaymentService paymentService;

	private final SubscriptionService subscriptions;

	private final FeaturedService featured;

	private final DepositService deposits;

	private final UserService users;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	PlatformPaymentService(PaymentRepository payments, PaymentService paymentService, SubscriptionService subscriptions,
			FeaturedService featured, DepositService deposits, UserService users, ApplicationEventPublisher events,
			Clock clock) {
		this.payments = payments;
		this.paymentService = paymentService;
		this.subscriptions = subscriptions;
		this.featured = featured;
		this.deposits = deposits;
		this.users = users;
		this.events = events;
		this.clock = clock;
	}

	// Venue owners

	@Transactional
	PaymentResponse claimInvoice(UUID userId, UUID invoiceId, PaymentRequests.Claim request) {
		SubscriptionInvoice invoice = this.subscriptions.invoiceToPay(userId, invoiceId);
		if (this.payments.existsByInvoiceIdAndStatus(invoiceId, TransactionStatus.PENDING)) {
			throw alreadyPending();
		}
		requireClaimable(request, invoice.getAmount());
		Payment payment = this.payments.save(Payment.forInvoice(invoice, userId, request.method(),
				this.paymentService.uniqueTransactionId(request.method(), request.transactionId()), null, null));
		return PaymentResponse.from(payment, this.subscriptions.venueNameOf(invoice));
	}

	@Transactional
	PaymentResponse claimListing(UUID userId, UUID listingId, PaymentRequests.Claim request) {
		FeaturedListing listing = this.featured.listingToPay(userId, listingId);
		if (this.payments.existsByListingIdAndStatus(listingId, TransactionStatus.PENDING)) {
			throw alreadyPending();
		}
		requireClaimable(request, listing.getPrice());
		Payment payment = this.payments.save(Payment.forListing(listing, userId, request.method(),
				this.paymentService.uniqueTransactionId(request.method(), request.transactionId()), null, null));
		return PaymentResponse.from(payment);
	}

	// Players

	@Transactional
	PaymentResponse claimDeposit(UUID userId, PaymentRequests.Claim request) {
		if (this.payments.existsByPurposeAndPayerUserIdAndStatus(PaymentPurpose.DEPOSIT, userId,
				TransactionStatus.PENDING)) {
			throw alreadyPending();
		}
		requireClaimable(request, this.deposits.topUpDue(userId));
		Payment payment = this.payments.save(Payment.forDeposit(userId, request.amount(), request.method(),
				this.paymentService.uniqueTransactionId(request.method(), request.transactionId())));
		return response(payment);
	}

	// Admins

	@Transactional(readOnly = true)
	List<PaymentResponse> pending() {
		return this.payments.findByStatusAndPurposeInOrderByCreatedAt(TransactionStatus.PENDING, PLATFORM)
			.stream()
			.map(this::response)
			.toList();
	}

	@Transactional
	PaymentResponse approve(UUID adminId, UUID paymentId) {
		Payment payment = pendingPlatform(paymentId);
		Instant now = this.clock.instant();
		if (payment.getPurpose() == PaymentPurpose.DEPOSIT) {
			payment.approve(adminId, now);
			this.deposits.toppedUp(payment.getPayerUserId(), payment.getId(), payment.getAmount());
		}
		else if (payment.getInvoice() != null) {
			SubscriptionInvoice invoice = this.subscriptions.openInvoice(payment.getInvoice().getId());
			payment.approve(adminId, now);
			this.subscriptions.invoicePaid(invoice);
		}
		else {
			FeaturedListing listing = this.featured.unpaidListing(payment.getListing().getId());
			payment.approve(adminId, now);
			this.featured.listingPaid(listing);
		}
		return response(payment);
	}

	@Transactional
	PaymentResponse reject(UUID adminId, UUID paymentId, String reason) {
		Payment payment = pendingPlatform(paymentId);
		payment.reject(adminId, reason.strip());
		this.events.publishEvent(new PlatformPaymentRejected(payment.getPayerUserId(), payment.getAmount(),
				(payment.getPurpose() == PaymentPurpose.DEPOSIT) ? "your booking deposit"
						: (payment.getInvoice() != null) ? "your " + payment.getInvoice().getInvoiceNumber() + " invoice"
								: "your featured listing",
				reason.strip()));
		return response(payment);
	}

	/** Money FutSlot took some other way (a bank transfer it can see, cash). */
	@Transactional
	PaymentResponse recordInvoice(UUID adminId, UUID invoiceId, PaymentRequests.RecordEntry request) {
		SubscriptionInvoice invoice = this.subscriptions.openInvoice(invoiceId);
		Payment payment = this.payments.save(Payment.forInvoice(invoice, this.subscriptions.ownerOf(invoice),
				request.method(), transactionId(request), adminId, this.clock.instant()));
		this.subscriptions.invoicePaid(invoice);
		return response(payment);
	}

	@Transactional
	PaymentResponse recordListing(UUID adminId, UUID listingId, PaymentRequests.RecordEntry request) {
		FeaturedListing listing = this.featured.unpaidListing(listingId);
		Payment payment = this.payments.save(Payment.forListing(listing, listing.getVenue().getOwnerId(),
				request.method(), transactionId(request), adminId, this.clock.instant()));
		this.featured.listingPaid(listing);
		return response(payment);
	}

	private void requireClaimable(PaymentRequests.Claim request, BigDecimal amount) {
		if (!PaymentService.CLAIMABLE.contains(request.method())) {
			throw PaymentService.badRequest("INVALID_METHOD", "Pay with eSewa, Khalti, Fonepay or bank transfer");
		}
		if (request.amount().compareTo(amount) != 0) {
			throw PaymentService.badRequest("AMOUNT_MISMATCH", "The amount due is Rs " + amount.toPlainString());
		}
	}

	private String transactionId(PaymentRequests.RecordEntry request) {
		if (!PaymentService.RECORDABLE.contains(request.method())) {
			throw PaymentService.badRequest("INVALID_METHOD", "Use cash, eSewa, Khalti, Fonepay or bank transfer");
		}
		return (request.transactionId() != null && !request.transactionId().isBlank())
				? this.paymentService.uniqueTransactionId(request.method(), request.transactionId()) : null;
	}

	private Payment pendingPlatform(UUID paymentId) {
		Payment payment = this.payments.findById(paymentId)
			.filter(Payment::isPlatform)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found"));
		if (payment.getStatus() != TransactionStatus.PENDING) {
			throw new ApiException(HttpStatus.CONFLICT, "INVALID_PAYMENT_STATUS", "This payment is " + payment.getStatus());
		}
		return payment;
	}

	/** Names who paid: the venue for an invoice, the player for a deposit top-up. */
	private PaymentResponse response(Payment payment) {
		String payer = (payment.getInvoice() != null) ? this.subscriptions.venueNameOf(payment.getInvoice())
				: (payment.getPurpose() == PaymentPurpose.DEPOSIT) ? this.users.get(payment.getPayerUserId()).getFullName()
						: null;
		return PaymentResponse.from(payment, payer);
	}

	private static ApiException alreadyPending() {
		return new ApiException(HttpStatus.CONFLICT, "PAYMENT_PENDING", "Your payment is already being verified");
	}

}
