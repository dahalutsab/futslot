package com.futslot.venue;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;
import com.futslot.common.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/admin/venues")
@PreAuthorize("hasRole('ADMIN')")
@Validated
class AdminVenueController {

	private final AdminVenueService adminVenueService;

	AdminVenueController(AdminVenueService adminVenueService) {
		this.adminVenueService = adminVenueService;
	}

	@GetMapping
	PageResponse<VenueSummaryResponse> list(@RequestParam(defaultValue = "PENDING_APPROVAL") VenueStatus status,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return this.adminVenueService.list(status, PageRequest.of(page, size, Sort.by("updatedAt")));
	}

	@GetMapping("/{venueId}")
	VenueResponse get(@PathVariable UUID venueId) {
		return this.adminVenueService.get(venueId);
	}

	@PostMapping("/{venueId}/approve")
	VenueResponse approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		return this.adminVenueService.approve(CurrentUser.id(jwt), venueId);
	}

	@PostMapping("/{venueId}/reject")
	VenueResponse reject(@PathVariable UUID venueId, @Valid @RequestBody ModerationRequest request) {
		return this.adminVenueService.reject(venueId, request.reason());
	}

	@PostMapping("/{venueId}/suspend")
	VenueResponse suspend(@PathVariable UUID venueId, @Valid @RequestBody ModerationRequest request) {
		return this.adminVenueService.suspend(venueId, request.reason());
	}

	@PostMapping("/{venueId}/reinstate")
	VenueResponse reinstate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		return this.adminVenueService.reinstate(CurrentUser.id(jwt), venueId);
	}

}
