package com.futslot.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import com.futslot.billing.FeaturedListing;
import com.futslot.billing.SubscriptionInvoice;
import com.futslot.booking.Booking;
import com.futslot.membership.Membership;
import com.futslot.tournament.Registration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Money paid to a venue for a booking, a membership or a tournament entry, or by a venue to FutSlot for
 * a subscription or featured listing ({@code venueId} is then null: it isn't the venue's income). Today
 * money goes straight to the payee's own wallet/QR or cash, so FutSlot only records and verifies payments; {@code transactionId} is the wallet's transaction
 * id (unique per method, so the same receipt can't be claimed twice).
 */
@Entity
@Table(name = "payments")
public class Payment {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private PaymentPurpose purpose;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "booking_id", updatable = false)
	private Booking booking;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "membership_id", updatable = false)
	private Membership membership;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tournament_registration_id", updatable = false)
	private Registration entry;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "subscription_invoice_id", updatable = false)
	private SubscriptionInvoice invoice;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "featured_listing_id", updatable = false)
	private FeaturedListing listing;

	@Column(updatable = false)
	private UUID venueId;

	@Column(updatable = false)
	private UUID payerUserId;

	@Column(nullable = false, precision = 12, scale = 2, updatable = false)
	private BigDecimal amount;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, updatable = false, length = 3)
	private String currency = "NPR";

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private PaymentMethod method;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TransactionStatus status;

	@Column(name = "gateway_transaction_id", updatable = false)
	private String transactionId;

	private String failureReason;

	private UUID recordedBy;

	private Instant paidAt;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected Payment() {
	}

	private Payment(PaymentPurpose purpose, Booking booking, Membership membership, UUID venueId, UUID payerUserId,
			BigDecimal amount, PaymentMethod method, String transactionId, TransactionStatus status) {
		this.purpose = purpose;
		this.booking = booking;
		this.membership = membership;
		this.venueId = venueId;
		this.payerUserId = payerUserId;
		this.amount = amount;
		this.method = method;
		this.transactionId = transactionId;
		this.status = status;
	}

	/** A player says they paid the venue; staff verify it against their wallet. */
	static Payment claimed(Booking booking, UUID payerUserId, BigDecimal amount, PaymentMethod method,
			String transactionId) {
		return new Payment(PaymentPurpose.BOOKING, booking, null, booking.getVenue().getId(), payerUserId, amount,
				method, transactionId, TransactionStatus.PENDING);
	}

	/** Staff recorded money they received (cash at the counter, or a transfer they can see). */
	static Payment received(Booking booking, BigDecimal amount, PaymentMethod method, String transactionId,
			UUID recordedBy, Instant at) {
		Payment payment = new Payment(PaymentPurpose.BOOKING, booking, null, booking.getVenue().getId(),
				booking.getCustomer().getUserId(), amount, method, transactionId, TransactionStatus.SUCCEEDED);
		payment.approve(recordedBy, at);
		return payment;
	}

	static Payment membershipClaimed(Membership membership, UUID payerUserId, BigDecimal amount, PaymentMethod method,
			String transactionId) {
		return new Payment(PaymentPurpose.MEMBERSHIP, null, membership, membership.getVenue().getId(), payerUserId,
				amount, method, transactionId, TransactionStatus.PENDING);
	}

	static Payment membershipReceived(Membership membership, BigDecimal amount, PaymentMethod method,
			String transactionId, UUID recordedBy, Instant at) {
		Payment payment = new Payment(PaymentPurpose.MEMBERSHIP, null, membership, membership.getVenue().getId(),
				membership.getCustomer().getUserId(), amount, method, transactionId, TransactionStatus.SUCCEEDED);
		payment.approve(recordedBy, at);
		return payment;
	}

	static Payment entryClaimed(Registration entry, UUID payerUserId, BigDecimal amount, PaymentMethod method,
			String transactionId) {
		Payment payment = new Payment(PaymentPurpose.TOURNAMENT_ENTRY, null, null,
				entry.getTournament().getVenue().getId(), payerUserId, amount, method, transactionId,
				TransactionStatus.PENDING);
		payment.entry = entry;
		return payment;
	}

	static Payment entryReceived(Registration entry, BigDecimal amount, PaymentMethod method, String transactionId,
			UUID recordedBy, Instant at) {
		Payment payment = new Payment(PaymentPurpose.TOURNAMENT_ENTRY, null, null,
				entry.getTournament().getVenue().getId(), entry.getRegisteredBy(), amount, method, transactionId,
				TransactionStatus.SUCCEEDED);
		payment.entry = entry;
		payment.approve(recordedBy, at);
		return payment;
	}

	/** A venue owner paying FutSlot for an invoice; {@code recordedBy} set means an admin took it directly. */
	static Payment forInvoice(SubscriptionInvoice invoice, UUID payerUserId, PaymentMethod method, String transactionId,
			UUID recordedBy, Instant at) {
		Payment payment = new Payment(PaymentPurpose.SUBSCRIPTION, null, null, null, payerUserId, invoice.getAmount(),
				method, transactionId, (recordedBy != null) ? TransactionStatus.SUCCEEDED : TransactionStatus.PENDING);
		payment.invoice = invoice;
		if (recordedBy != null) {
			payment.approve(recordedBy, at);
		}
		return payment;
	}

	static Payment forListing(FeaturedListing listing, UUID payerUserId, PaymentMethod method, String transactionId,
			UUID recordedBy, Instant at) {
		Payment payment = new Payment(PaymentPurpose.FEATURED_LISTING, null, null, null, payerUserId, listing.getPrice(),
				method, transactionId, (recordedBy != null) ? TransactionStatus.SUCCEEDED : TransactionStatus.PENDING);
		payment.listing = listing;
		if (recordedBy != null) {
			payment.approve(recordedBy, at);
		}
		return payment;
	}

	void approve(UUID by, Instant at) {
		this.status = TransactionStatus.SUCCEEDED;
		this.recordedBy = by;
		this.paidAt = at;
	}

	void reject(UUID by, String reason) {
		this.status = TransactionStatus.FAILED;
		this.recordedBy = by;
		this.failureReason = reason;
	}

	void cancel(String reason) {
		this.status = TransactionStatus.CANCELLED;
		this.failureReason = reason;
	}

	void refunded(BigDecimal totalRefunded) {
		this.status = (totalRefunded.compareTo(this.amount) >= 0) ? TransactionStatus.REFUNDED
				: TransactionStatus.PARTIALLY_REFUNDED;
	}

	public UUID getId() {
		return this.id;
	}

	public PaymentPurpose getPurpose() {
		return this.purpose;
	}

	public Booking getBooking() {
		return this.booking;
	}

	public Membership getMembership() {
		return this.membership;
	}

	public Registration getEntry() {
		return this.entry;
	}

	public SubscriptionInvoice getInvoice() {
		return this.invoice;
	}

	public FeaturedListing getListing() {
		return this.listing;
	}

	/** Paid to FutSlot rather than to a venue. */
	boolean isPlatform() {
		return this.purpose == PaymentPurpose.SUBSCRIPTION || this.purpose == PaymentPurpose.FEATURED_LISTING;
	}

	public UUID getVenueId() {
		return this.venueId;
	}

	public UUID getPayerUserId() {
		return this.payerUserId;
	}

	public BigDecimal getAmount() {
		return this.amount;
	}

	public PaymentMethod getMethod() {
		return this.method;
	}

	public TransactionStatus getStatus() {
		return this.status;
	}

	public String getTransactionId() {
		return this.transactionId;
	}

	public String getFailureReason() {
		return this.failureReason;
	}

	public Instant getPaidAt() {
		return this.paidAt;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

}
