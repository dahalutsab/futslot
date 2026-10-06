package com.futslot.booking;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/owner/venues/{venueId}/recurring-bookings")
class RecurringBookingController {

	private final RecurringBookingService service;

	RecurringBookingController(RecurringBookingService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	RecurringBookingResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@Valid @RequestBody RecurringBookingRequest request) {
		return this.service.create(CurrentUser.id(jwt), venueId, request);
	}

	@GetMapping
	List<RecurringBookingResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		return this.service.list(CurrentUser.id(jwt), venueId);
	}

	@GetMapping("/{seriesId}")
	RecurringBookingResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID seriesId) {
		return this.service.get(CurrentUser.id(jwt), venueId, seriesId);
	}

	@PostMapping("/{seriesId}/pause")
	RecurringBookingResponse pause(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID seriesId) {
		return this.service.pause(CurrentUser.id(jwt), venueId, seriesId);
	}

	@PostMapping("/{seriesId}/resume")
	RecurringBookingResponse resume(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID seriesId) {
		return this.service.resume(CurrentUser.id(jwt), venueId, seriesId);
	}

	@PostMapping("/{seriesId}/cancel")
	RecurringBookingResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID seriesId) {
		return this.service.cancel(CurrentUser.id(jwt), venueId, seriesId);
	}

}
