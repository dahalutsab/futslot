package com.futslot.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * What a venue can subscribe to. FREE (seeded) is the default for every venue without a subscription.
 * Limits left empty mean unlimited. Commission isn't charged: FutSlot doesn't handle venues' money.
 */
@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan {

	static final String FREE = "FREE";

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false, unique = true, updatable = false)
	private String code;

	@Column(nullable = false)
	private String name;

	private String description;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal monthlyPrice;

	@Column(precision = 12, scale = 2)
	private BigDecimal yearlyPrice;

	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal commissionPercent = BigDecimal.ZERO;

	private Integer maxCourts;

	private Integer maxStaff;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(nullable = false, columnDefinition = "text[]")
	private String[] features = new String[0];

	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false)
	private int sortOrder;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected SubscriptionPlan() {
	}

	SubscriptionPlan(String code, PlanRequest request) {
		this.code = code;
		apply(request);
	}

	void apply(PlanRequest request) {
		this.name = request.name().strip();
		this.description = (request.description() != null && !request.description().isBlank())
				? request.description().strip() : null;
		this.monthlyPrice = request.monthlyPrice();
		this.yearlyPrice = request.yearlyPrice();
		this.maxCourts = request.maxCourts();
		this.maxStaff = request.maxStaff();
		this.features = (request.features() != null) ? request.features().stream().map(String::strip).distinct()
			.toArray(String[]::new) : new String[0];
		if (request.active() != null) {
			this.active = request.active();
		}
		this.sortOrder = (request.sortOrder() != null) ? request.sortOrder() : this.sortOrder;
	}

	/** The price per period, or null if the plan isn't sold on that cycle. */
	BigDecimal price(BillingCycle cycle) {
		return (cycle == BillingCycle.MONTHLY) ? this.monthlyPrice : this.yearlyPrice;
	}

	boolean isFree() {
		return FREE.equals(this.code);
	}

	public UUID getId() {
		return this.id;
	}

	public String getCode() {
		return this.code;
	}

	public String getName() {
		return this.name;
	}

	public String getDescription() {
		return this.description;
	}

	public BigDecimal getMonthlyPrice() {
		return this.monthlyPrice;
	}

	public BigDecimal getYearlyPrice() {
		return this.yearlyPrice;
	}

	public Integer getMaxCourts() {
		return this.maxCourts;
	}

	public Integer getMaxStaff() {
		return this.maxStaff;
	}

	public List<String> getFeatures() {
		return List.of(this.features);
	}

	public boolean isActive() {
		return this.active;
	}

	public int getSortOrder() {
		return this.sortOrder;
	}

}
