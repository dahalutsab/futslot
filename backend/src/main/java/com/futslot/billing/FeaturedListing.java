package com.futslot.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.futslot.venue.Venue;

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

/** A venue paying to be shown first for a number of days. */
@Entity
@Table(name = "featured_listings")
public class FeaturedListing {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "venue_id", updatable = false)
	private Venue venue;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private FeaturedPlacement placement;

	@Column(updatable = false)
	private String city;

	@Column(nullable = false)
	private Instant startsAt;

	@Column(nullable = false)
	private Instant endsAt;

	@Column(nullable = false, precision = 12, scale = 2, updatable = false)
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private FeaturedStatus status = FeaturedStatus.PENDING_PAYMENT;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected FeaturedListing() {
	}

	FeaturedListing(Venue venue, FeaturedPlacement placement, Instant startsAt, Instant endsAt, BigDecimal price) {
		this.venue = venue;
		this.placement = placement;
		this.city = (placement == FeaturedPlacement.CITY_TOP) ? venue.getCity() : null;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.price = price;
	}

	/** Paid: shown from {@code startsAt}. */
	void paid(Instant now) {
		this.status = now.isBefore(this.startsAt) ? FeaturedStatus.SCHEDULED : FeaturedStatus.ACTIVE;
	}

	void setStatus(FeaturedStatus status) {
		this.status = status;
	}

	/** SCHEDULED/ACTIVE as it stands at {@code now}, before the hourly job catches up. */
	FeaturedStatus statusAt(Instant now) {
		if (this.status == FeaturedStatus.SCHEDULED || this.status == FeaturedStatus.ACTIVE) {
			if (!now.isBefore(this.endsAt)) {
				return FeaturedStatus.ENDED;
			}
			return now.isBefore(this.startsAt) ? FeaturedStatus.SCHEDULED : FeaturedStatus.ACTIVE;
		}
		return this.status;
	}

	public UUID getId() {
		return this.id;
	}

	public Venue getVenue() {
		return this.venue;
	}

	public FeaturedPlacement getPlacement() {
		return this.placement;
	}

	public String getCity() {
		return this.city;
	}

	public Instant getStartsAt() {
		return this.startsAt;
	}

	public Instant getEndsAt() {
		return this.endsAt;
	}

	public BigDecimal getPrice() {
		return this.price;
	}

	public FeaturedStatus getStatus() {
		return this.status;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

}
