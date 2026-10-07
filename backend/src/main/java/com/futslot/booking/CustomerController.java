package com.futslot.booking;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.booking.CustomerService.CustomerSort;
import com.futslot.common.CurrentUser;
import com.futslot.common.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/owner/venues/{venueId}/customers")
@Validated
class CustomerController {

	private final CustomerService customerService;

	CustomerController(CustomerService customerService) {
		this.customerService = customerService;
	}

	/** {@code q} matches name or phone; {@code sort} is RECENT (default), FREQUENT or NAME. */
	@GetMapping
	PageResponse<CustomerSummaryResponse> search(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@RequestParam(required = false) String q, @RequestParam(defaultValue = "RECENT") CustomerSort sort,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return this.customerService.search(CurrentUser.id(jwt), venueId, q, sort, page, size);
	}

	@GetMapping("/{customerId}")
	CustomerDetailResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID customerId) {
		return this.customerService.get(CurrentUser.id(jwt), venueId, customerId);
	}

	@PatchMapping("/{customerId}")
	CustomerDetailResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID customerId, @Valid @RequestBody CustomerUpdateRequest request) {
		return this.customerService.update(CurrentUser.id(jwt), venueId, customerId, request);
	}

}
