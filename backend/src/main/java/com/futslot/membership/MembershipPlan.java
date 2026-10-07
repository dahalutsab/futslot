package com.futslot.membership;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
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

/** Something a venue sells ahead of play: a game pack or a discount pass. */
@Entity
@Table(name = "membership_plans")
public class MembershipPlan {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false, updatable = false)
	private UUID venueId;

	@Column(nullable = false)
	private String name;

	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MembershipType type;

	private Integer gamesIncluded;

	private Integer discountPercent;

	@Column(nullable = false)
	private int validityDays;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected MembershipPlan() {
	}

	MembershipPlan(UUID venueId, MembershipPlanRequest request) {
		this.venueId = venueId;
		apply(request);
	}

	void apply(MembershipPlanRequest request) {
		this.name = request.name().strip();
		this.description = (request.description() != null && !request.description().isBlank())
				? request.description().strip() : null;
		this.type = request.type();
		this.gamesIncluded = request.gamesIncluded();
		this.discountPercent = request.discountPercent();
		this.validityDays = request.validityDays();
		this.price = request.price();
		if (request.active() != null) {
			this.active = request.active();
		}
	}

	/** True if {@code request} would change what members already bought. */
	boolean changesBenefits(MembershipPlanRequest request) {
		return this.type != request.type() || !Objects.equals(this.gamesIncluded, request.gamesIncluded())
				|| !Objects.equals(this.discountPercent, request.discountPercent())
				|| this.validityDays != request.validityDays();
	}

	public UUID getId() {
		return this.id;
	}

	public UUID getVenueId() {
		return this.venueId;
	}

	public String getName() {
		return this.name;
	}

	public String getDescription() {
		return this.description;
	}

	public MembershipType getType() {
		return this.type;
	}

	public Integer getGamesIncluded() {
		return this.gamesIncluded;
	}

	public Integer getDiscountPercent() {
		return this.discountPercent;
	}

	public int getValidityDays() {
		return this.validityDays;
	}

	public BigDecimal getPrice() {
		return this.price;
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
