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

@RestController
@RequestMapping("/api/v1/owner/venues/{venueId}/staff")
class StaffController {

	private final StaffService staffService;

	StaffController(StaffService staffService) {
		this.staffService = staffService;
	}

	@GetMapping
	List<StaffResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		return this.staffService.list(CurrentUser.id(jwt), venueId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	StaffResponse add(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@Valid @RequestBody StaffRequest request) {
		return this.staffService.add(CurrentUser.id(jwt), venueId, request);
	}

	@PutMapping("/{staffId}")
	StaffResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId, @PathVariable UUID staffId,
			@Valid @RequestBody StaffUpdateRequest request) {
		return this.staffService.update(CurrentUser.id(jwt), venueId, staffId, request);
	}

	@DeleteMapping("/{staffId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId, @PathVariable UUID staffId) {
		this.staffService.remove(CurrentUser.id(jwt), venueId, staffId);
	}

}
