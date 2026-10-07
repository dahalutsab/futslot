package com.futslot.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** API shapes for FutSlot's own billing. */
public final class BillingViews {

	private BillingViews() {
	}

	public record Plan(UUID id, String code, String name, String description, BigDecimal monthlyPrice,
			BigDecimal yearlyPrice, Integer maxCourts, Integer maxStaff, List<String> features, boolean active) {

		static Plan from(SubscriptionPlan plan) {
			return new Plan(plan.getId(), plan.getCode(), plan.getName(), plan.getDescription(), plan.getMonthlyPrice(),
					plan.getYearlyPrice(), plan.getMaxCourts(), plan.getMaxStaff(), plan.getFeatures(), plan.isActive());
		}

	}

	/**
	 * A venue's plan as the owner sees it. Without a subscription the venue is on FREE and the subscription
	 * fields are null. {@code limitsEnforced} is false while FutSlot lets every venue use everything.
	 */
	public record Subscription(UUID venueId, Plan plan, SubscriptionStatus status, BillingCycle billingCycle,
			Instant currentPeriodStart, Instant currentPeriodEnd, boolean cancelAtPeriodEnd, Plan nextPlan,
			BillingCycle nextBillingCycle, Invoice openInvoice, boolean limitsEnforced, long courts, long staff,
			String paymentInstructions) {

	}

	/** {@code paymentPending}: the owner has sent a payment that FutSlot hasn't checked yet. */
	public record Invoice(UUID id, String invoiceNumber, UUID venueId, String planCode, String planName,
			BillingCycle billingCycle, BigDecimal amount, Instant periodStart, Instant periodEnd, LocalDate dueOn,
			InvoiceStatus status, Instant paidAt, boolean paymentPending) {

		static Invoice from(SubscriptionInvoice invoice, boolean paymentPending) {
			SubscriptionPlan plan = invoice.getPlan();
			return new Invoice(invoice.getId(), invoice.getInvoiceNumber(), invoice.getSubscription().getVenueId(),
					(plan != null) ? plan.getCode() : null, (plan != null) ? plan.getName() : null,
					invoice.getBillingCycle(), invoice.getAmount(), invoice.getPeriodStart(), invoice.getPeriodEnd(),
					invoice.getDueOn(), invoice.getStatus(), invoice.getPaidAt(), paymentPending);
		}

	}

	public record Featured(UUID id, UUID venueId, String venueName, FeaturedPlacement placement, String city,
			Instant startsAt, Instant endsAt, BigDecimal price, FeaturedStatus status, String paymentInstructions) {

	}

	/** Price and whether a place is free for those days. */
	public record FeaturedQuote(FeaturedPlacement placement, LocalDate startsOn, int days, BigDecimal pricePerDay,
			BigDecimal price, boolean available) {

	}

}
