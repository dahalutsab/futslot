package com.futslot.membership;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.futslot.booking.VenueCustomer;
import com.futslot.common.Timestamps;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * A plan bought by one of the venue's customers. Valid from {@code startsOn} to {@code expiresOn}
 * inclusive, in the venue's local dates; {@code gamesRemaining} is null when games are unlimited.
 */
@Entity
@Table(name = "memberships")
public class Membership {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "plan_id", updatable = false)
	private MembershipPlan plan;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "venue_id", updatable = false)
	private Venue venue;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", updatable = false)
	private VenueCustomer customer;

	@Column(nullable = false)
	private LocalDate startsOn;

	@Column(nullable = false)
	private LocalDate expiresOn;

	private Integer gamesRemaining;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MembershipStatus status = MembershipStatus.PENDING_PAYMENT;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected Membership() {
	}

	/** Dates are provisional until it's paid for and activated. */
	Membership(MembershipPlan plan, Venue venue, VenueCustomer customer, LocalDate today) {
		this.plan = plan;
		this.venue = venue;
		this.customer = customer;
		this.gamesRemaining = plan.getGamesIncluded();
		setValidity(today);
	}

	/** Paid for: valid from today for the plan's number of days. */
	public void activate(LocalDate today) {
		setValidity(today);
		this.status = MembershipStatus.ACTIVE;
	}

	public void cancel() {
		this.status = MembershipStatus.CANCELLED;
	}

	void useGame() {
		if (this.gamesRemaining != null) {
			this.gamesRemaining--;
		}
	}

	void returnGame() {
		if (this.gamesRemaining != null) {
			this.gamesRemaining++;
		}
	}

	/** ACTIVE memberships past their last day read as EXPIRED before the nightly job catches up. */
	public MembershipStatus statusOn(LocalDate today) {
		return (this.status == MembershipStatus.ACTIVE && today.isAfter(this.expiresOn)) ? MembershipStatus.EXPIRED
				: this.status;
	}

	private void setValidity(LocalDate from) {
		this.startsOn = from;
		this.expiresOn = from.plusDays(this.plan.getValidityDays() - 1L);
	}

	public UUID getId() {
		return this.id;
	}

	public MembershipPlan getPlan() {
		return this.plan;
	}

	public Venue getVenue() {
		return this.venue;
	}

	public VenueCustomer getCustomer() {
		return this.customer;
	}

	public LocalDate getStartsOn() {
		return this.startsOn;
	}

	public LocalDate getExpiresOn() {
		return this.expiresOn;
	}

	public Integer getGamesRemaining() {
		return this.gamesRemaining;
	}

	public MembershipStatus getStatus() {
		return this.status;
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
