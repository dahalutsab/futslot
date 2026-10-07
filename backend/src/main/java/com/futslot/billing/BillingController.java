package com.futslot.billing;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;
import com.futslot.venue.VenueSummaryResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Plans and featured venues are public; a venue's subscription and listings are for its owner. */
@RestController
@RequestMapping("/api/v1")
@Validated
class BillingController {

	private final SubscriptionService subscriptionService;

	private final FeaturedService featuredService;

	BillingController(SubscriptionService subscriptionService, FeaturedService featuredService) {
		this.subscriptionService = subscriptionService;
		this.featuredService = featuredService;
	}

	@GetMapping("/subscription-plans")
	@SecurityRequirements
	List<BillingViews.Plan> plans() {
		return this.subscriptionService.plansOnSale();
	}

	/** For the "featured" strip and home carousel. */
	@GetMapping("/featured-venues")
	@SecurityRequirements
	List<VenueSummaryResponse> featured(@RequestParam(defaultValue = "SEARCH_TOP") FeaturedPlacement placement,
			@RequestParam(required = false) String city) {
		return this.featuredService.live(placement, city);
	}

	@GetMapping("/owner/venues/{venueId}/subscription")
	BillingViews.Subscription subscription(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		return this.subscriptionService.current(CurrentUser.id(jwt), venueId);
	}

	@PutMapping("/owner/venues/{venueId}/subscription")
	BillingViews.Subscription choose(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@Valid @RequestBody BillingRequests.ChoosePlan request) {
		return this.subscriptionService.choose(CurrentUser.id(jwt), venueId, request);
	}

	@GetMapping("/owner/venues/{venueId}/invoices")
	List<BillingViews.Invoice> invoices(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		return this.subscriptionService.invoices(CurrentUser.id(jwt), venueId);
	}

	@GetMapping("/owner/venues/{venueId}/featured/quote")
	BillingViews.FeaturedQuote quote(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@RequestParam FeaturedPlacement placement,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startsOn,
			@RequestParam @Min(1) @Max(30) int days) {
		return this.featuredService.quote(CurrentUser.id(jwt), venueId, placement, startsOn, days);
	}

	@GetMapping("/owner/venues/{venueId}/featured")
	List<BillingViews.Featured> listings(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		return this.featuredService.forVenue(CurrentUser.id(jwt), venueId);
	}

	@PostMapping("/owner/venues/{venueId}/featured")
	@ResponseStatus(HttpStatus.CREATED)
	BillingViews.Featured feature(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@Valid @RequestBody BillingRequests.Feature request) {
		return this.featuredService.book(CurrentUser.id(jwt), venueId, request);
	}

	@PostMapping("/owner/venues/{venueId}/featured/{listingId}/cancel")
	BillingViews.Featured cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID listingId) {
		return this.featuredService.cancel(CurrentUser.id(jwt), venueId, listingId);
	}

}
