package com.futslot.billing;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Admins define plans and see what venues owe FutSlot. Verifying payments is under /admin/payments. */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminBillingController {

	private final SubscriptionService subscriptionService;

	private final FeaturedService featuredService;

	AdminBillingController(SubscriptionService subscriptionService, FeaturedService featuredService) {
		this.subscriptionService = subscriptionService;
		this.featuredService = featuredService;
	}

	@GetMapping("/subscription-plans")
	List<BillingViews.Plan> plans() {
		return this.subscriptionService.allPlans();
	}

	@PostMapping("/subscription-plans")
	@ResponseStatus(HttpStatus.CREATED)
	BillingViews.Plan create(@Valid @RequestBody PlanRequest request) {
		return this.subscriptionService.createPlan(request);
	}

	@PutMapping("/subscription-plans/{planId}")
	BillingViews.Plan update(@PathVariable UUID planId, @Valid @RequestBody PlanRequest request) {
		return this.subscriptionService.updatePlan(planId, request);
	}

	/** Unpaid by default. */
	@GetMapping("/invoices")
	List<BillingViews.Invoice> invoices(@RequestParam(required = false) Set<InvoiceStatus> status) {
		return this.subscriptionService.invoicesByStatus(
				(status != null && !status.isEmpty()) ? status : EnumSet.of(InvoiceStatus.DUE, InvoiceStatus.OVERDUE));
	}

	/** Waiting for payment, scheduled and live by default. */
	@GetMapping("/featured-listings")
	List<BillingViews.Featured> featured(@RequestParam(required = false) Set<FeaturedStatus> status) {
		return this.featuredService.byStatus((status != null && !status.isEmpty()) ? status
				: EnumSet.of(FeaturedStatus.PENDING_PAYMENT, FeaturedStatus.SCHEDULED, FeaturedStatus.ACTIVE));
	}

}
