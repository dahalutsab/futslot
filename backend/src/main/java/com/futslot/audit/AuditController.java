package com.futslot.audit;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;
import com.futslot.common.PageResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Newest first. */
@RestController
@RequestMapping("/api/v1")
@Validated
class AuditController {

	private final AuditService auditService;

	AuditController(AuditService auditService) {
		this.auditService = auditService;
	}

	/** Owner only: what staff and FutSlot did at the venue. */
	@GetMapping("/owner/venues/{venueId}/audit-log")
	PageResponse<AuditView> forVenue(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
		return this.auditService.forVenue(CurrentUser.id(jwt), venueId, PageRequest.of(page, size));
	}

	@GetMapping("/admin/audit-log")
	@PreAuthorize("hasRole('ADMIN')")
	PageResponse<AuditView> forAdmin(@RequestParam(required = false) UUID venueId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
		return this.auditService.forAdmin(venueId, PageRequest.of(page, size));
	}

}
