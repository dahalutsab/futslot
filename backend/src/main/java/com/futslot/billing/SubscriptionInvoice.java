package com.futslot.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

/** What a venue owes FutSlot for one subscription period. */
@Entity
@Table(name = "subscription_invoices")
public class SubscriptionInvoice {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "subscription_id", updatable = false)
	private VenueSubscription subscription;

	@Column(nullable = false, unique = true, updatable = false)
	private String invoiceNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "plan_id", updatable = false)
	private SubscriptionPlan plan;

	@Enumerated(EnumType.STRING)
	@Column(updatable = false)
	private BillingCycle billingCycle;

	@Column(nullable = false, precision = 12, scale = 2, updatable = false)
	private BigDecimal amount;

	@Column(nullable = false)
	private Instant periodStart;

	@Column(nullable = false)
	private Instant periodEnd;

	@Column(nullable = false)
	private LocalDate dueOn;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private InvoiceStatus status = InvoiceStatus.DUE;

	private Instant paidAt;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected SubscriptionInvoice() {
	}

	SubscriptionInvoice(VenueSubscription subscription, String invoiceNumber, SubscriptionPlan plan, BillingCycle cycle,
			Instant periodStart, LocalDate dueOn) {
		this.subscription = subscription;
		this.invoiceNumber = invoiceNumber;
		this.plan = plan;
		this.billingCycle = cycle;
		this.amount = plan.price(cycle);
		this.periodStart = periodStart;
		this.periodEnd = cycle.after(periodStart);
		this.dueOn = dueOn;
	}

	/** A first invoice paid late covers a period starting when it's paid, not when it was raised. */
	void startPeriodAt(Instant start) {
		this.periodStart = start;
		this.periodEnd = this.billingCycle.after(start);
	}

	void paid(Instant at) {
		this.status = InvoiceStatus.PAID;
		this.paidAt = at;
	}

	void setStatus(InvoiceStatus status) {
		this.status = status;
	}

	public UUID getId() {
		return this.id;
	}

	public VenueSubscription getSubscription() {
		return this.subscription;
	}

	public String getInvoiceNumber() {
		return this.invoiceNumber;
	}

	public SubscriptionPlan getPlan() {
		return this.plan;
	}

	public BillingCycle getBillingCycle() {
		return this.billingCycle;
	}

	public BigDecimal getAmount() {
		return this.amount;
	}

	public Instant getPeriodStart() {
		return this.periodStart;
	}

	public Instant getPeriodEnd() {
		return this.periodEnd;
	}

	public LocalDate getDueOn() {
		return this.dueOn;
	}

	public InvoiceStatus getStatus() {
		return this.status;
	}

	public Instant getPaidAt() {
		return this.paidAt;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

}
