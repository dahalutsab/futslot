package com.futslot.venue;

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

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
class AddOnController {

	private final AddOnService addOnService;

	AddOnController(AddOnService addOnService) {
		this.addOnService = addOnService;
	}

	@GetMapping("/venues/{slug}/add-ons")
	@SecurityRequirements
	List<AddOnResponse> onSale(@PathVariable String slug) {
		return this.addOnService.onSale(slug);
	}

	@GetMapping("/owner/venues/{venueId}/add-ons")
	List<AddOnResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		return this.addOnService.forVenue(CurrentUser.id(jwt), venueId);
	}

	@PostMapping("/owner/venues/{venueId}/add-ons")
	@ResponseStatus(HttpStatus.CREATED)
	AddOnResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@Valid @RequestBody AddOnRequest request) {
		return this.addOnService.create(CurrentUser.id(jwt), venueId, request);
	}

	/** Set {@code active} false to stop selling it. */
	@PutMapping("/owner/venues/{venueId}/add-ons/{itemId}")
	AddOnResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId, @PathVariable UUID itemId,
			@Valid @RequestBody AddOnRequest request) {
		return this.addOnService.update(CurrentUser.id(jwt), venueId, itemId, request);
	}

}
