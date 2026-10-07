package com.futslot.billing;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

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

/**
 * A venue on a paid plan. Starts PAST_DUE until the first invoice is paid; each paid invoice covers one
 * period. A plan change waits for the renewal ({@code nextPlan}).
 */
@Entity
@Table(name = "venue_subscriptions")
public class VenueSubscription {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false, updatable = false)
	private UUID venueId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "plan_id")
	private SubscriptionPlan plan;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BillingCycle billingCycle;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SubscriptionStatus status = SubscriptionStatus.PAST_DUE;

	private Instant trialEndsAt;

	@Column(nullable = false)
	private Instant currentPeriodStart;

	@Column(nullable = false)
	private Instant currentPeriodEnd;

	@Column(nullable = false)
	private boolean cancelAtPeriodEnd;

	private Instant cancelledAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "next_plan_id")
	private SubscriptionPlan nextPlan;

	@Enumerated(EnumType.STRING)
	private BillingCycle nextBillingCycle;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected VenueSubscription() {
	}

	VenueSubscription(UUID venueId, SubscriptionPlan plan, BillingCycle cycle, Instant now) {
		this.venueId = venueId;
		this.plan = plan;
		this.billingCycle = cycle;
		this.currentPeriodStart = now;
		this.currentPeriodEnd = cycle.after(now);
	}

	/** Not yet paid for at all: the plan can still be swapped outright. */
	void replacePlan(SubscriptionPlan plan, BillingCycle cycle, Instant now) {
		this.plan = plan;
		this.billingCycle = cycle;
		this.currentPeriodStart = now;
		this.currentPeriodEnd = cycle.after(now);
	}

	/** An invoice for {@code start} to {@code end} was paid: that is now the current period. */
	void paidFor(SubscriptionPlan plan, BillingCycle cycle, Instant start, Instant end) {
		this.plan = plan;
		this.billingCycle = cycle;
		this.currentPeriodStart = start;
		this.currentPeriodEnd = end;
		this.status = SubscriptionStatus.ACTIVE;
		this.nextPlan = null;
		this.nextBillingCycle = null;
	}

	void changeAtRenewal(SubscriptionPlan plan, BillingCycle cycle) {
		this.nextPlan = plan;
		this.nextBillingCycle = cycle;
		this.cancelAtPeriodEnd = false;
	}

	void cancelAtPeriodEnd(boolean cancel) {
		this.cancelAtPeriodEnd = cancel;
		if (cancel) {
			this.nextPlan = null;
			this.nextBillingCycle = null;
		}
	}

	void end(SubscriptionStatus status, Instant at) {
		this.status = status;
		this.cancelledAt = at;
	}

	void pastDue() {
		this.status = SubscriptionStatus.PAST_DUE;
	}

	/** The plan the next period is billed on. */
	SubscriptionPlan renewalPlan() {
		return (this.nextPlan != null) ? this.nextPlan : this.plan;
	}

	BillingCycle renewalCycle() {
		return (this.nextBillingCycle != null) ? this.nextBillingCycle : this.billingCycle;
	}

	public UUID getId() {
		return this.id;
	}

	public UUID getVenueId() {
		return this.venueId;
	}

	public SubscriptionPlan getPlan() {
		return this.plan;
	}

	public BillingCycle getBillingCycle() {
		return this.billingCycle;
	}

	public SubscriptionStatus getStatus() {
		return this.status;
	}

	public Instant getCurrentPeriodStart() {
		return this.currentPeriodStart;
	}

	public Instant getCurrentPeriodEnd() {
		return this.currentPeriodEnd;
	}

	public boolean isCancelAtPeriodEnd() {
		return this.cancelAtPeriodEnd;
	}

	public SubscriptionPlan getNextPlan() {
		return this.nextPlan;
	}

	public BillingCycle getNextBillingCycle() {
		return this.nextBillingCycle;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

}
