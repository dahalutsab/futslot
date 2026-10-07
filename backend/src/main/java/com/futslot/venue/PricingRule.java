package com.futslot.venue;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.futslot.common.DayOfWeekConverter;
import com.futslot.common.Timestamps;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

/** Price of a court within a local time window; null day means every day. Highest priority wins. */
@Entity
@Table(name = "pricing_rules")
public class PricingRule {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "court_id", updatable = false)
	private Court court;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PriceLabel label;

	@Convert(converter = DayOfWeekConverter.class)
	private DayOfWeek dayOfWeek;

	@Column(nullable = false)
	private LocalTime startTime;

	@Column(nullable = false)
	private LocalTime endTime;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(nullable = false)
	private int priority;

	private LocalDate validFrom;

	private LocalDate validTo;

	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected PricingRule() {
	}

	PricingRule(Court court, PricingRuleRequest request) {
		this.court = court;
		apply(request);
	}

	void apply(PricingRuleRequest request) {
		this.label = (request.label() != null) ? request.label() : PriceLabel.STANDARD;
		this.dayOfWeek = request.dayOfWeek();
		this.startTime = request.startTime();
		this.endTime = request.endTime();
		this.price = request.price();
		this.priority = (request.priority() != null) ? request.priority() : 0;
		this.validFrom = request.validFrom();
		this.validTo = request.validTo();
		if (request.active() != null) {
			this.active = request.active();
		}
	}

	public UUID getId() {
		return this.id;
	}

	public Court getCourt() {
		return this.court;
	}

	public PriceLabel getLabel() {
		return this.label;
	}

	public DayOfWeek getDayOfWeek() {
		return this.dayOfWeek;
	}

	public LocalTime getStartTime() {
		return this.startTime;
	}

	public LocalTime getEndTime() {
		return this.endTime;
	}

	public BigDecimal getPrice() {
		return this.price;
	}

	public int getPriority() {
		return this.priority;
	}

	public LocalDate getValidFrom() {
		return this.validFrom;
	}

	public LocalDate getValidTo() {
		return this.validTo;
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
