package com.futslot.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.futslot.common.Timestamps;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Money a venue owes back after a cancellation; the venue pays it and marks it done. */
@Entity
@Table(name = "refunds")
public class Refund {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "payment_id", updatable = false)
	private Payment payment;

	@Column(nullable = false, precision = 12, scale = 2, updatable = false)
	private BigDecimal amount;

	@Column(nullable = false, updatable = false)
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RefundStatus status = RefundStatus.REQUESTED;

	/** How the venue paid it back, e.g. the wallet transaction id. */
	@Column(name = "gateway_refund_id")
	private String reference;

	@Column(updatable = false)
	private UUID requestedBy;

	private UUID processedBy;

	private Instant processedAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected Refund() {
	}

	Refund(Payment payment, BigDecimal amount, String reason, UUID requestedBy) {
		this.payment = payment;
		this.amount = amount;
		this.reason = reason;
		this.requestedBy = requestedBy;
	}

	void complete(UUID by, String reference, Instant at) {
		this.status = RefundStatus.SUCCEEDED;
		this.processedBy = by;
		this.reference = reference;
		this.processedAt = at;
	}

	public UUID getId() {
		return this.id;
	}

	public Payment getPayment() {
		return this.payment;
	}

	public BigDecimal getAmount() {
		return this.amount;
	}

	public String getReason() {
		return this.reason;
	}

	public RefundStatus getStatus() {
		return this.status;
	}

	public String getReference() {
		return this.reference;
	}

	public Instant getProcessedAt() {
		return this.processedAt;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	@PrePersist
	void stampCreated() {
		if (this.createdAt == null) {
			this.createdAt = Timestamps.now();
		}
	}

}
