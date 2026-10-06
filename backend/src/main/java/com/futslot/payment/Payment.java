package com.futslot.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import com.futslot.booking.Booking;

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
 * Money paid to a venue. Today players pay the venue's own wallet/QR or cash, so FutSlot only records
 * and verifies payments; {@code transactionId} is the wallet's transaction id (unique per method, so the
 * same receipt can't be claimed twice).
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

	private Payment(Booking booking, UUID payerUserId, BigDecimal amount, PaymentMethod method, String transactionId,
			TransactionStatus status) {
		this.purpose = PaymentPurpose.BOOKING;
		this.booking = booking;
		this.venueId = booking.getVenue().getId();
		this.payerUserId = payerUserId;
		this.amount = amount;
		this.method = method;
		this.transactionId = transactionId;
		this.status = status;
	}

	/** A player says they paid the venue; staff verify it against their wallet. */
	static Payment claimed(Booking booking, UUID payerUserId, BigDecimal amount, PaymentMethod method,
			String transactionId) {
		return new Payment(booking, payerUserId, amount, method, transactionId, TransactionStatus.PENDING);
	}

	/** Staff recorded money they received (cash at the counter, or a transfer they can see). */
	static Payment received(Booking booking, BigDecimal amount, PaymentMethod method, String transactionId,
			UUID recordedBy, Instant at) {
		Payment payment = new Payment(booking, booking.getCustomer().getUserId(), amount, method, transactionId,
				TransactionStatus.SUCCEEDED);
		payment.recordedBy = recordedBy;
		payment.paidAt = at;
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

	public Booking getBooking() {
		return this.booking;
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
