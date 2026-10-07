package com.futslot.discount;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.futslot.common.Timestamps;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** A promo code a venue gives out. {@code code} is stored upper-case; players can type any case. */
@Entity
@Table(name = "discount_codes")
public class DiscountCode {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(updatable = false)
	private UUID venueId;

	@Column(nullable = false, updatable = false)
	private String code;

	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private DiscountType type;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal value;

	@Column(precision = 12, scale = 2)
	private BigDecimal maxDiscountAmount;

	@Column(precision = 12, scale = 2)
	private BigDecimal minBookingAmount;

	private Instant validFrom;

	private Instant validTo;

	private Integer usageLimit;

	private Integer perUserLimit;

	@Column(nullable = false)
	private int usedCount;

	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false, updatable = false)
	private UUID createdBy;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected DiscountCode() {
	}

	DiscountCode(UUID venueId, DiscountCodeRequest request, UUID createdBy) {
		this.venueId = venueId;
		this.code = normalize(request.code());
		this.type = request.type();
		this.createdBy = createdBy;
		apply(request);
	}

	void apply(DiscountCodeRequest request) {
		this.description = (request.description() != null && !request.description().isBlank())
				? request.description().strip() : null;
		this.value = request.value();
		this.maxDiscountAmount = request.maxDiscountAmount();
		this.minBookingAmount = request.minBookingAmount();
		this.validFrom = (request.validFrom() != null) ? request.validFrom().toInstant() : null;
		this.validTo = (request.validTo() != null) ? request.validTo().toInstant() : null;
		this.usageLimit = request.usageLimit();
		this.perUserLimit = request.perUserLimit();
		if (request.active() != null) {
			this.active = request.active();
		}
	}

	/** What this code takes off {@code price}: never more than the price itself. */
	BigDecimal discountOn(BigDecimal price) {
		BigDecimal off = (this.type == DiscountType.PERCENT)
				? price.multiply(this.value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP) : this.value;
		if (this.maxDiscountAmount != null) {
			off = off.min(this.maxDiscountAmount);
		}
		return off.min(price);
	}

	void redeemed() {
		this.usedCount++;
	}

	void released() {
		this.usedCount = Math.max(0, this.usedCount - 1);
	}

	static String normalize(String code) {
		return code.strip().toUpperCase(Locale.ROOT);
	}

	public UUID getId() {
		return this.id;
	}

	public UUID getVenueId() {
		return this.venueId;
	}

	public String getCode() {
		return this.code;
	}

	public String getDescription() {
		return this.description;
	}

	public DiscountType getType() {
		return this.type;
	}

	public BigDecimal getValue() {
		return this.value;
	}

	public BigDecimal getMaxDiscountAmount() {
		return this.maxDiscountAmount;
	}

	public BigDecimal getMinBookingAmount() {
		return this.minBookingAmount;
	}

	public Instant getValidFrom() {
		return this.validFrom;
	}

	public Instant getValidTo() {
		return this.validTo;
	}

	public Integer getUsageLimit() {
		return this.usageLimit;
	}

	public Integer getPerUserLimit() {
		return this.perUserLimit;
	}

	public int getUsedCount() {
		return this.usedCount;
	}

	public boolean isActive() {
		return this.active;
	}

	@PrePersist
	void stampCreated() {
		if (this.createdAt == null) {
			this.createdAt = Timestamps.now();
		}
	}

}
