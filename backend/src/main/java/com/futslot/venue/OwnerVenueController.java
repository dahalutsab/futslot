package com.futslot.venue;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;

import jakarta.validation.Valid;

/** Venue setup for owners. Any signed-in user can create a venue and becomes its owner. */
@RestController
@RequestMapping("/api/v1/owner/venues")
class OwnerVenueController {

	private final VenueService venueService;

	OwnerVenueController(VenueService venueService) {
		this.venueService = venueService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	VenueResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody VenueDetails request) {
		return this.venueService.create(CurrentUser.id(jwt), request);
	}

	@GetMapping
	List<VenueSummaryResponse> list(@AuthenticationPrincipal Jwt jwt) {
		return this.venueService.listOwned(CurrentUser.id(jwt));
	}

	@GetMapping("/{venueId}")
	VenueResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		return this.venueService.getOwned(CurrentUser.id(jwt), venueId);
	}

	@PutMapping("/{venueId}")
	VenueResponse updateDetails(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@Valid @RequestBody VenueDetails request) {
		return this.venueService.updateDetails(CurrentUser.id(jwt), venueId, request);
	}

	@PutMapping("/{venueId}/policy")
	VenueResponse updatePolicy(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@Valid @RequestBody VenuePolicy request) {
		return this.venueService.updatePolicy(CurrentUser.id(jwt), venueId, request);
	}

	@PutMapping("/{venueId}/operating-hours")
	VenueResponse replaceOperatingHours(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@RequestBody List<@Valid DayHours> request) {
		return this.venueService.replaceOperatingHours(CurrentUser.id(jwt), venueId, request);
	}

	@PutMapping("/{venueId}/amenities")
	VenueResponse replaceAmenities(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@Valid @RequestBody AmenitiesRequest request) {
		return this.venueService.replaceAmenities(CurrentUser.id(jwt), venueId, request.codes());
	}

	@PostMapping("/{venueId}/submit")
	VenueResponse submit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		return this.venueService.submitForApproval(CurrentUser.id(jwt), venueId);
	}

	@PostMapping("/{venueId}/courts")
	@ResponseStatus(HttpStatus.CREATED)
	CourtResponse addCourt(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@Valid @RequestBody CourtRequest request) {
		return this.venueService.addCourt(CurrentUser.id(jwt), venueId, request);
	}

	@PutMapping("/{venueId}/courts/{courtId}")
	CourtResponse updateCourt(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID courtId, @Valid @RequestBody CourtRequest request) {
		return this.venueService.updateCourt(CurrentUser.id(jwt), venueId, courtId, request);
	}

	@PostMapping("/{venueId}/courts/{courtId}/pricing-rules")
	@ResponseStatus(HttpStatus.CREATED)
	PricingRuleResponse addPricingRule(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID courtId, @Valid @RequestBody PricingRuleRequest request) {
		return this.venueService.addPricingRule(CurrentUser.id(jwt), venueId, courtId, request);
	}

	@PutMapping("/{venueId}/courts/{courtId}/pricing-rules/{ruleId}")
	PricingRuleResponse updatePricingRule(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID courtId, @PathVariable UUID ruleId, @Valid @RequestBody PricingRuleRequest request) {
		return this.venueService.updatePricingRule(CurrentUser.id(jwt), venueId, courtId, ruleId, request);
	}

	@DeleteMapping("/{venueId}/courts/{courtId}/pricing-rules/{ruleId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deletePricingRule(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId, @PathVariable UUID courtId,
			@PathVariable UUID ruleId) {
		this.venueService.deletePricingRule(CurrentUser.id(jwt), venueId, courtId, ruleId);
	}

}
