package com.futslot.site;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;

import jakarta.validation.Valid;

/** A venue setting up its website. */
@RestController
@RequestMapping("/api/v1/owner/venues/{venueId}/site")
class SiteController {

	private final SiteService siteService;

	SiteController(SiteService siteService) {
		this.siteService = siteService;
	}

	@GetMapping
	SiteSettings get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		return this.siteService.get(CurrentUser.id(jwt), venueId);
	}

	@PutMapping
	SiteSettings save(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId, @Valid @RequestBody SiteRequest request) {
		return this.siteService.save(CurrentUser.id(jwt), venueId, request);
	}

	@PostMapping("/publish")
	SiteSettings publish(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		return this.siteService.publish(CurrentUser.id(jwt), venueId, true);
	}

	@PostMapping("/unpublish")
	SiteSettings unpublish(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		return this.siteService.publish(CurrentUser.id(jwt), venueId, false);
	}

	/** Whether an address is free, and if not, why. */
	@GetMapping("/subdomain-check")
	SubdomainCheck check(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId, @RequestParam String subdomain) {
		this.siteService.get(CurrentUser.id(jwt), venueId);
		String problem = this.siteService.subdomainProblem(SiteService.subdomain(subdomain), venueId);
		return new SubdomainCheck(problem == null, problem);
	}

	record SubdomainCheck(boolean available, String problem) {
	}

}
