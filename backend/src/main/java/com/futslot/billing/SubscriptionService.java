package com.futslot.billing;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.ApiException;
import com.futslot.venue.Court;
import com.futslot.venue.PlanLimits;
import com.futslot.venue.Venue;
import com.futslot.venue.VenueAccess;
import com.futslot.venue.VenueRepository;

/**
 * Venue subscriptions to FutSlot's plans. Every venue starts on FREE (no subscription row). Choosing a paid
 * plan raises an invoice; paying it (verified by a FutSlot admin, since venues pay FutSlot's own wallet)
 * starts the period. Renewal invoices go out {@code renewalNoticeDays} before the period ends; an unpaid
 * renewal gives {@code graceDays} before the venue drops back to FREE. Plan changes wait for renewal.
 */
@Service
public class SubscriptionService implements PlanLimits {

	private static final ZoneId NEPAL = ZoneId.of("Asia/Kathmandu");

	private static final Set<InvoiceStatus> OPEN = EnumSet.of(InvoiceStatus.DUE, InvoiceStatus.OVERDUE);

	private static final SecureRandom RANDOM = new SecureRandom();

	private final SubscriptionPlanRepository plans;

	private final VenueSubscriptionRepository subscriptions;

	private final SubscriptionInvoiceRepository invoices;

	private final VenueRepository venues;

	private final VenueAccess access;

	private final BillingProperties properties;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	SubscriptionService(SubscriptionPlanRepository plans, VenueSubscriptionRepository subscriptions,
			SubscriptionInvoiceRepository invoices, VenueRepository venues, VenueAccess access,
			BillingProperties properties, ApplicationEventPublisher events, Clock clock) {
		this.plans = plans;
		this.subscriptions = subscriptions;
		this.invoices = invoices;
		this.venues = venues;
		this.access = access;
		this.properties = properties;
		this.events = events;
		this.clock = clock;
	}

	// Plans (admins)

	@Transactional
	BillingViews.Plan createPlan(PlanRequest request) {
		if (request.code() == null) {
			throw badRequest("CODE_REQUIRED", "Give the plan a code such as PRO");
		}
		if (this.plans.existsByCode(request.code())) {
			throw conflict("PLAN_EXISTS", "There's already a plan " + request.code());
		}
		return BillingViews.Plan.from(this.plans.save(new SubscriptionPlan(request.code(), request)));
	}

	/** The code can't change. A price change applies from the next invoice. */
	@Transactional
	BillingViews.Plan updatePlan(UUID planId, PlanRequest request) {
		SubscriptionPlan plan = this.plans.findById(planId).orElseThrow(SubscriptionService::planNotFound);
		if (plan.isFree() && request.monthlyPrice().signum() > 0) {
			throw badRequest("FREE_IS_FREE", "The FREE plan can't have a price");
		}
		plan.apply(request);
		return BillingViews.Plan.from(plan);
	}

	@Transactional(readOnly = true)
	List<BillingViews.Plan> allPlans() {
		return this.plans.findAllByOrderBySortOrderAscMonthlyPriceAsc().stream().map(BillingViews.Plan::from).toList();
	}

	@Transactional(readOnly = true)
	List<BillingViews.Plan> plansOnSale() {
		return this.plans.findByActiveTrueOrderBySortOrderAscMonthlyPriceAsc().stream().map(BillingViews.Plan::from).toList();
	}

	// Venue owners

	@Transactional(readOnly = true)
	BillingViews.Subscription current(UUID userId, UUID venueId) {
		return view(this.access.requireOwner(userId, venueId));
	}

	@Transactional
	BillingViews.Subscription choose(UUID userId, UUID venueId, BillingRequests.ChoosePlan request) {
		Venue venue = this.access.requireOwner(userId, venueId);
		SubscriptionPlan plan = this.plans.findByCode(request.planCode().strip().toUpperCase())
			.filter(SubscriptionPlan::isActive)
			.orElseThrow(SubscriptionService::planNotFound);
		VenueSubscription live = this.subscriptions.findLive(venueId).orElse(null);
		Instant now = this.clock.instant();
		if (plan.isFree()) {
			if (live != null) {
				stop(live, now);
			}
			return view(venue);
		}
		BillingCycle cycle = (request.billingCycle() != null) ? request.billingCycle() : BillingCycle.MONTHLY;
		if (plan.price(cycle) == null) {
			throw badRequest("CYCLE_NOT_OFFERED", plan.getName() + " isn't sold " + cycle.name().toLowerCase());
		}
		if (live == null) {
			VenueSubscription subscription = this.subscriptions.save(new VenueSubscription(venueId, plan, cycle, now));
			raise(subscription, venue, plan, cycle, now, today(now));
		}
		else if (!everPaid(live)) {
			voidOpen(live);
			live.replacePlan(plan, cycle, now);
			raise(live, venue, plan, cycle, now, today(now));
		}
		else if (plan.getId().equals(live.getPlan().getId()) && cycle == live.getBillingCycle()) {
			// Staying on the current plan: undo any pending change or cancellation
			live.changeAtRenewal(null, null);
			live.cancelAtPeriodEnd(false);
			reissueRenewal(live, venue, now);
		}
		else {
			live.changeAtRenewal(plan, cycle);
			reissueRenewal(live, venue, now);
		}
		return view(venue);
	}

	@Transactional(readOnly = true)
	List<BillingViews.Invoice> invoices(UUID userId, UUID venueId) {
		this.access.requireOwner(userId, venueId);
		return views(this.invoices.findForVenue(venueId));
	}

	// Admins

	@Transactional(readOnly = true)
	List<BillingViews.Invoice> invoicesByStatus(Set<InvoiceStatus> statuses) {
		return views(this.invoices.findByStatusInOrderByDueOn(statuses));
	}

	// For payments

	/** An unpaid invoice of a venue {@code userId} owns. */
	@Transactional(readOnly = true)
	public SubscriptionInvoice invoiceToPay(UUID userId, UUID invoiceId) {
		SubscriptionInvoice invoice = this.invoices.findById(invoiceId).orElseThrow(SubscriptionService::invoiceNotFound);
		this.access.requireOwner(userId, invoice.getSubscription().getVenueId());
		return requireOpen(invoice);
	}

	@Transactional(readOnly = true)
	public SubscriptionInvoice openInvoice(UUID invoiceId) {
		return requireOpen(this.invoices.findById(invoiceId).orElseThrow(SubscriptionService::invoiceNotFound));
	}

	/**
	 * The money is in. A first invoice starts the subscription from now; a renewal paid after the period
	 * ended starts the new period straight away; one paid early waits for the current period to end.
	 */
	@Transactional
	public void invoicePaid(SubscriptionInvoice invoice) {
		Instant now = this.clock.instant();
		VenueSubscription subscription = this.subscriptions.lockById(invoice.getSubscription().getId()).orElseThrow();
		boolean first = !everPaid(subscription);
		invoice.paid(now);
		if (first) {
			invoice.startPeriodAt(now);
			subscription.paidFor(invoice.getPlan(), invoice.getBillingCycle(), invoice.getPeriodStart(),
					invoice.getPeriodEnd());
		}
		else if (!now.isBefore(subscription.getCurrentPeriodEnd())) {
			subscription.paidFor(invoice.getPlan(), invoice.getBillingCycle(), invoice.getPeriodStart(),
					invoice.getPeriodEnd());
		}
		publish(BillingEvent.Type.SUBSCRIPTION_ACTIVE, subscription, invoice.getPlan(), invoice.getAmount(),
				invoice.getPeriodEnd().atZone(NEPAL).toLocalDate());
	}

	/** Owner of the venue an invoice is for. */
	public UUID ownerOf(SubscriptionInvoice invoice) {
		return venue(invoice.getSubscription().getVenueId()).getOwnerId();
	}

	public String venueNameOf(SubscriptionInvoice invoice) {
		return venue(invoice.getSubscription().getVenueId()).getName();
	}

	// Limits

	@Override
	@Transactional(readOnly = true)
	public void checkCourts(UUID venueId, long courts) {
		SubscriptionPlan plan = effectivePlan(venueId);
		if (this.properties.enforceLimits() && plan.getMaxCourts() != null && courts >= plan.getMaxCourts()) {
			throw limitReached(plan, plan.getMaxCourts() + ((plan.getMaxCourts() == 1) ? " court" : " courts"));
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void checkStaff(UUID venueId, long staffCount) {
		SubscriptionPlan plan = effectivePlan(venueId);
		if (this.properties.enforceLimits() && plan.getMaxStaff() != null && staffCount >= plan.getMaxStaff()) {
			throw limitReached(plan, (plan.getMaxStaff() == 0) ? "no staff accounts" : plan.getMaxStaff() + " staff");
		}
	}

	// Billing run

	/**
	 * Marks overdue invoices, raises renewals, rolls paid periods over, and ends subscriptions that were
	 * cancelled or not paid in time. Runs hourly from {@link BillingJobs}; safe to run any number of times.
	 */
	@Transactional
	public void runBilling() {
		Instant now = this.clock.instant();
		LocalDate today = today(now);
		this.invoices.findOverdue(today).forEach(invoice -> invoice.setStatus(InvoiceStatus.OVERDUE));
		Duration notice = Duration.ofDays(this.properties.renewalNoticeDays());
		Duration grace = Duration.ofDays(this.properties.graceDays());
		for (VenueSubscription subscription : this.subscriptions.findByStatusIn(
				EnumSet.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE))) {
			Venue venue = venue(subscription.getVenueId());
			boolean renewingToFree = subscription.isCancelAtPeriodEnd() || subscription.renewalPlan().isFree();
			if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
				if (!now.isBefore(subscription.getCurrentPeriodEnd())) {
					SubscriptionInvoice renewal = this.invoices
						.findForPeriod(subscription.getId(), subscription.getCurrentPeriodEnd())
						.orElse(null);
					if (renewingToFree) {
						end(subscription, SubscriptionStatus.CANCELLED, now);
					}
					else if (renewal != null && renewal.getStatus() == InvoiceStatus.PAID) {
						subscription.paidFor(renewal.getPlan(), renewal.getBillingCycle(), renewal.getPeriodStart(),
								renewal.getPeriodEnd());
					}
					else {
						subscription.pastDue();
						publish(BillingEvent.Type.SUBSCRIPTION_PAST_DUE, subscription, subscription.getPlan(), null,
								subscription.getCurrentPeriodEnd().plus(grace).atZone(NEPAL).toLocalDate());
					}
				}
				else if (!renewingToFree && !now.isBefore(subscription.getCurrentPeriodEnd().minus(notice))
						&& this.invoices.findForPeriod(subscription.getId(), subscription.getCurrentPeriodEnd()).isEmpty()) {
					raise(subscription, venue, subscription.renewalPlan(), subscription.renewalCycle(),
							subscription.getCurrentPeriodEnd(), subscription.getCurrentPeriodEnd().atZone(NEPAL).toLocalDate());
				}
			}
			else {
				Instant deadline = everPaid(subscription) ? subscription.getCurrentPeriodEnd().plus(grace)
						: subscription.getCreatedAt().plus(grace);
				if (!now.isBefore(deadline)) {
					end(subscription, SubscriptionStatus.EXPIRED, now);
				}
			}
		}
	}

	private List<BillingViews.Invoice> views(List<SubscriptionInvoice> invoices) {
		if (invoices.isEmpty()) {
			return List.of();
		}
		Set<UUID> pending = new HashSet<>(this.invoices.withPendingPayment(invoices.stream().map(SubscriptionInvoice::getId).toList()));
		return invoices.stream().map(invoice -> BillingViews.Invoice.from(invoice, pending.contains(invoice.getId()))).toList();
	}

	private BillingViews.Subscription view(Venue venue) {
		VenueSubscription live = this.subscriptions.findLive(venue.getId()).orElse(null);
		SubscriptionPlan plan = effectivePlan(venue.getId());
		long courts = venue.getCourts().stream().filter(Court::isActive).count();
		long staffCount = this.access.activeStaff(venue.getId());
		if (live == null) {
			return new BillingViews.Subscription(venue.getId(), BillingViews.Plan.from(plan), null, null, null, null, false,
					null, null, null, this.properties.enforceLimits(), courts, staffCount,
					this.properties.paymentInstructions());
		}
		SubscriptionInvoice open = this.invoices.findBySubscriptionIdAndStatusIn(live.getId(), OPEN)
			.stream()
			.findFirst()
			.orElse(null);
		return new BillingViews.Subscription(venue.getId(), BillingViews.Plan.from(live.getPlan()), live.getStatus(),
				live.getBillingCycle(), live.getCurrentPeriodStart(), live.getCurrentPeriodEnd(),
				live.isCancelAtPeriodEnd(), (live.getNextPlan() != null) ? BillingViews.Plan.from(live.getNextPlan()) : null,
				live.getNextBillingCycle(), (open != null) ? views(List.of(open)).getFirst() : null,
				this.properties.enforceLimits(), courts, staffCount, this.properties.paymentInstructions());
	}

	/** What the venue gets now: its paid plan while paid up (or in grace), otherwise FREE. */
	private SubscriptionPlan effectivePlan(UUID venueId) {
		return this.subscriptions.findLive(venueId)
			.filter(live -> live.getStatus() == SubscriptionStatus.ACTIVE || everPaid(live))
			.map(VenueSubscription::getPlan)
			.orElseGet(this::freePlan);
	}

	/** Going back to FREE: never-paid subscriptions stop now, paid ones at the end of the period. */
	private void stop(VenueSubscription live, Instant now) {
		if (!everPaid(live)) {
			end(live, SubscriptionStatus.CANCELLED, now);
		}
		else {
			live.cancelAtPeriodEnd(true);
			this.invoices.findForPeriod(live.getId(), live.getCurrentPeriodEnd())
				.filter(invoice -> invoice.getStatus().isOpen())
				.ifPresent(invoice -> invoice.setStatus(InvoiceStatus.VOID));
		}
	}

	/** A renewal invoice already out is replaced so it bills the plan the venue now wants. */
	private void reissueRenewal(VenueSubscription live, Venue venue, Instant now) {
		SubscriptionInvoice renewal = this.invoices.findForPeriod(live.getId(), live.getCurrentPeriodEnd()).orElse(null);
		if (renewal == null || !renewal.getStatus().isOpen()) {
			return;
		}
		renewal.setStatus(InvoiceStatus.VOID);
		this.invoices.flush();
		if (!live.renewalPlan().isFree()) {
			raise(live, venue, live.renewalPlan(), live.renewalCycle(), live.getCurrentPeriodEnd(), renewal.getDueOn());
		}
	}

	private void end(VenueSubscription subscription, SubscriptionStatus status, Instant now) {
		voidOpen(subscription);
		subscription.end(status, now);
		publish(BillingEvent.Type.SUBSCRIPTION_ENDED, subscription, subscription.getPlan(), null, null);
	}

	private void voidOpen(VenueSubscription subscription) {
		this.invoices.findBySubscriptionIdAndStatusIn(subscription.getId(), OPEN)
			.forEach(invoice -> invoice.setStatus(InvoiceStatus.VOID));
		this.invoices.flush();
	}

	private void raise(VenueSubscription subscription, Venue venue, SubscriptionPlan plan, BillingCycle cycle,
			Instant periodStart, LocalDate dueOn) {
		SubscriptionInvoice invoice = this.invoices.save(new SubscriptionInvoice(subscription, invoiceNumber(), plan, cycle,
				periodStart, dueOn));
		this.events.publishEvent(new BillingEvent(BillingEvent.Type.INVOICE_DUE, venue.getId(), venue.getName(),
				venue.getOwnerId(), plan.getName(), invoice.getAmount(), dueOn));
	}

	private boolean everPaid(VenueSubscription subscription) {
		return this.invoices.existsBySubscriptionIdAndStatus(subscription.getId(), InvoiceStatus.PAID);
	}

	private void publish(BillingEvent.Type type, VenueSubscription subscription, SubscriptionPlan plan,
			BigDecimal amount, LocalDate date) {
		Venue venue = venue(subscription.getVenueId());
		this.events.publishEvent(new BillingEvent(type, venue.getId(), venue.getName(), venue.getOwnerId(),
				plan.getName(), amount, date));
	}

	/** INV-20261007-48213 */
	private String invoiceNumber() {
		String date = DateTimeFormatter.BASIC_ISO_DATE.format(today(this.clock.instant()));
		String number;
		do {
			number = "INV-" + date + "-" + (10000 + RANDOM.nextInt(90000));
		}
		while (this.invoices.existsByInvoiceNumber(number));
		return number;
	}

	private SubscriptionPlan freePlan() {
		return this.plans.findByCode(SubscriptionPlan.FREE).orElseThrow();
	}

	private Venue venue(UUID venueId) {
		return this.venues.findById(venueId).orElseThrow();
	}

	private static LocalDate today(Instant now) {
		return now.atZone(NEPAL).toLocalDate();
	}

	private static SubscriptionInvoice requireOpen(SubscriptionInvoice invoice) {
		if (!invoice.getStatus().isOpen()) {
			throw conflict("INVOICE_NOT_OPEN", "This invoice is " + invoice.getStatus());
		}
		return invoice;
	}

	private static ApiException limitReached(SubscriptionPlan plan, String allowance) {
		return new ApiException(HttpStatus.FORBIDDEN, "PLAN_LIMIT",
				"The " + plan.getName() + " plan allows " + allowance + ". Upgrade to add more");
	}

	private static ApiException planNotFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "Plan not found");
	}

	private static ApiException invoiceNotFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found");
	}

	static ApiException conflict(String code, String message) {
		return new ApiException(HttpStatus.CONFLICT, code, message);
	}

	static ApiException badRequest(String code, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, code, message);
	}

}
