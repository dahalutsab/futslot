package com.futslot.membership;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import com.futslot.common.PageResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Buying and selling memberships involves money, so those endpoints live with payments. */
@RestController
@RequestMapping("/api/v1")
@Validated
class MembershipController {

	private final MembershipService membershipService;

	MembershipController(MembershipService membershipService) {
		this.membershipService = membershipService;
	}

	@GetMapping("/venues/{slug}/membership-plans")
	@SecurityRequirements
	List<MembershipPlanResponse> onSale(@PathVariable String slug) {
		return this.membershipService.plansOnSale(slug);
	}

	@GetMapping("/me/memberships")
	List<MembershipResponse> mine(@AuthenticationPrincipal Jwt jwt) {
		return this.membershipService.mine(CurrentUser.id(jwt));
	}

	// Venue

	@GetMapping("/owner/venues/{venueId}/membership-plans")
	List<MembershipPlanResponse> plans(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		return this.membershipService.plansForVenue(CurrentUser.id(jwt), venueId);
	}

	@PostMapping("/owner/venues/{venueId}/membership-plans")
	@ResponseStatus(HttpStatus.CREATED)
	MembershipPlanResponse createPlan(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@Valid @RequestBody MembershipPlanRequest request) {
		return this.membershipService.createPlan(CurrentUser.id(jwt), venueId, request);
	}

	@PutMapping("/owner/venues/{venueId}/membership-plans/{planId}")
	MembershipPlanResponse updatePlan(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID planId, @Valid @RequestBody MembershipPlanRequest request) {
		return this.membershipService.updatePlan(CurrentUser.id(jwt), venueId, planId, request);
	}

	@GetMapping("/owner/venues/{venueId}/memberships")
	PageResponse<MembershipResponse> memberships(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@RequestParam(required = false) MembershipStatus status, @RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return this.membershipService.forVenue(CurrentUser.id(jwt), venueId, status,
				PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"))));
	}

	@PostMapping("/owner/venues/{venueId}/memberships/{membershipId}/cancel")
	MembershipResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID membershipId) {
		return this.membershipService.cancel(CurrentUser.id(jwt), venueId, membershipId);
	}

}
