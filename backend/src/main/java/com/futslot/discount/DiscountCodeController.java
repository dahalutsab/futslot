package com.futslot.discount;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

@RestController
@RequestMapping("/api/v1/owner/venues/{venueId}/discount-codes")
class DiscountCodeController {

	private final DiscountCodeService discountCodeService;

	DiscountCodeController(DiscountCodeService discountCodeService) {
		this.discountCodeService = discountCodeService;
	}

	@GetMapping
	List<DiscountCodeResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		return this.discountCodeService.list(CurrentUser.id(jwt), venueId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	DiscountCodeResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@Valid @RequestBody DiscountCodeRequest request) {
		return this.discountCodeService.create(CurrentUser.id(jwt), venueId, request);
	}

	@PutMapping("/{codeId}")
	DiscountCodeResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId, @PathVariable UUID codeId,
			@Valid @RequestBody DiscountCodeRequest request) {
		return this.discountCodeService.update(CurrentUser.id(jwt), venueId, codeId, request);
	}

}
