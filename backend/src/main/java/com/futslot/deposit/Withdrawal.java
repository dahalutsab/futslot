package com.futslot.deposit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.futslot.common.Timestamps;
import com.futslot.payment.PaymentMethod;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * A player taking their whole deposit back. The money leaves the deposit straight away; FutSlot sends it to
 * {@code sendTo} once {@code dueAt} has passed and marks it paid.
 */
@Entity
@Table(name = "deposit_withdrawals")
class Withdrawal {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false, updatable = false)
	private UUID userId;

	@Column(nullable = false, precision = 12, scale = 2, updatable = false)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private PaymentMethod method;

	@Column(nullable = false, updatable = false)
	private String sendTo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private WithdrawalStatus status = WithdrawalStatus.REQUESTED;

	@Column(nullable = false, updatable = false)
	private Instant dueAt;

	private String reference;

	private UUID paidBy;

	private Instant paidAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected Withdrawal() {
	}

	Withdrawal(UUID userId, BigDecimal amount, PaymentMethod method, String sendTo, Instant dueAt) {
		this.userId = userId;
		this.amount = amount;
		this.method = method;
		this.sendTo = sendTo;
		this.dueAt = dueAt;
	}

	void paid(UUID by, String reference, Instant at) {
		this.status = WithdrawalStatus.PAID;
		this.paidBy = by;
		this.reference = reference;
		this.paidAt = at;
	}

	void cancel() {
		this.status = WithdrawalStatus.CANCELLED;
	}

	UUID getId() {
		return this.id;
	}

	UUID getUserId() {
		return this.userId;
	}

	BigDecimal getAmount() {
		return this.amount;
	}

	PaymentMethod getMethod() {
		return this.method;
	}

	String getSendTo() {
		return this.sendTo;
	}

	WithdrawalStatus getStatus() {
		return this.status;
	}

	Instant getDueAt() {
		return this.dueAt;
	}

	String getReference() {
		return this.reference;
	}

	Instant getPaidAt() {
		return this.paidAt;
	}

	Instant getCreatedAt() {
		return this.createdAt;
	}

	@PrePersist
	void stampCreated() {
		if (this.createdAt == null) {
			this.createdAt = Timestamps.now();
		}
	}

}
