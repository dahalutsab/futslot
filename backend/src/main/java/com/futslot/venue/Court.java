package com.futslot.venue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "courts")
public class Court {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "venue_id", updatable = false)
	private Venue venue;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CourtFormat format;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CourtSurface surface;

	@Column(nullable = false)
	private boolean isIndoor;

	@Column(nullable = false)
	private int slotDurationMinutes;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CourtStatus status = CourtStatus.ACTIVE;

	@Column(nullable = false)
	private int sortOrder;

	@OneToMany(mappedBy = "court")
	@OrderBy("dayOfWeek, startTime")
	private List<PricingRule> pricingRules = new ArrayList<>();

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected Court() {
	}

	Court(Venue venue, CourtRequest request) {
		this.venue = venue;
		apply(request);
	}

	void apply(CourtRequest request) {
		this.name = request.name().strip();
		this.format = request.format();
		this.surface = request.surface();
		this.isIndoor = Boolean.TRUE.equals(request.indoor());
		this.slotDurationMinutes = request.slotDurationMinutes();
		this.sortOrder = (request.sortOrder() != null) ? request.sortOrder() : this.sortOrder;
		if (request.active() != null) {
			this.status = request.active() ? CourtStatus.ACTIVE : CourtStatus.INACTIVE;
		}
	}

	public boolean isActive() {
		return this.status == CourtStatus.ACTIVE;
	}

	public UUID getId() {
		return this.id;
	}

	public Venue getVenue() {
		return this.venue;
	}

	public String getName() {
		return this.name;
	}

	public CourtFormat getFormat() {
		return this.format;
	}

	public CourtSurface getSurface() {
		return this.surface;
	}

	public boolean isIndoor() {
		return this.isIndoor;
	}

	public int getSlotDurationMinutes() {
		return this.slotDurationMinutes;
	}

	public CourtStatus getStatus() {
		return this.status;
	}

	public int getSortOrder() {
		return this.sortOrder;
	}

	public List<PricingRule> getPricingRules() {
		return this.pricingRules;
	}

	@PrePersist
	void stampCreated() {
		if (this.createdAt == null) {
			this.createdAt = Timestamps.now();
		}
	}

}
