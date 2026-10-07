package com.futslot.discount;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.futslot.common.Timestamps;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** A code used on a booking. Deleted again if the booking is cancelled or expires. */
@Entity
@Table(name = "discount_redemptions")
class DiscountRedemption {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false, updatable = false)
	private UUID discountCodeId;

	@Column(nullable = false, updatable = false)
	private UUID bookingId;

	@Column(updatable = false)
	private UUID userId;

	@Column(nullable = false, precision = 12, scale = 2, updatable = false)
	private BigDecimal amount;

	@Column(nullable = false, updatable = false)
	private Instant redeemedAt;

	protected DiscountRedemption() {
	}

	DiscountRedemption(UUID discountCodeId, UUID bookingId, UUID userId, BigDecimal amount) {
		this.discountCodeId = discountCodeId;
		this.bookingId = bookingId;
		this.userId = userId;
		this.amount = amount;
	}

	UUID getDiscountCodeId() {
		return this.discountCodeId;
	}

	@PrePersist
	void stampCreated() {
		if (this.redeemedAt == null) {
			this.redeemedAt = Timestamps.now();
		}
	}

}
