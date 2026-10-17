package com.futslot.deposit;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
 * What FutSlot owes a venue for one month of deposit fees. FutSlot takes no commission and the fees were
 * never refundable, so the net is the gross. An admin sends the money and marks it paid.
 */
@Entity
@Table(name = "payouts")
class Payout {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false, updatable = false)
	private UUID venueId;

	@Column(nullable = false, updatable = false)
	private LocalDate periodStart;

	@Column(nullable = false, updatable = false)
	private LocalDate periodEnd;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal grossAmount;

	@Column(nullable = false, precision = 12, scale = 2, updatable = false)
	private BigDecimal commissionAmount = BigDecimal.ZERO;

	@Column(nullable = false, precision = 12, scale = 2, updatable = false)
	private BigDecimal refundAmount = BigDecimal.ZERO;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal netAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PayoutStatus status = PayoutStatus.PENDING;

	@Enumerated(EnumType.STRING)
	private PaymentMethod method;

	private String transferReference;

	private UUID paidBy;

	private Instant paidAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected Payout() {
	}

	Payout(UUID venueId, LocalDate periodStart, LocalDate periodEnd, BigDecimal amount) {
		this.venueId = venueId;
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
		this.grossAmount = amount;
		this.netAmount = amount;
	}

	void add(BigDecimal amount) {
		this.grossAmount = this.grossAmount.add(amount);
		this.netAmount = this.netAmount.add(amount);
	}

	void paid(UUID by, PaymentMethod method, String reference, Instant at) {
		this.status = PayoutStatus.PAID;
		this.paidBy = by;
		this.method = method;
		this.transferReference = reference;
		this.paidAt = at;
	}

	UUID getId() {
		return this.id;
	}

	UUID getVenueId() {
		return this.venueId;
	}

	LocalDate getPeriodStart() {
		return this.periodStart;
	}

	LocalDate getPeriodEnd() {
		return this.periodEnd;
	}

	BigDecimal getNetAmount() {
		return this.netAmount;
	}

	PayoutStatus getStatus() {
		return this.status;
	}

	PaymentMethod getMethod() {
		return this.method;
	}

	String getTransferReference() {
		return this.transferReference;
	}

	Instant getPaidAt() {
		return this.paidAt;
	}

	@PrePersist
	void stampCreated() {
		if (this.createdAt == null) {
			this.createdAt = Timestamps.now();
		}
	}

}
