package com.futslot.slot;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.ApiException;
import com.futslot.common.CurrentUser;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
class AvailabilityController {

	private final AvailabilityService availabilityService;

	private final CourtBlockService courtBlockService;

	AvailabilityController(AvailabilityService availabilityService, CourtBlockService courtBlockService) {
		this.availabilityService = availabilityService;
		this.courtBlockService = courtBlockService;
	}

	@GetMapping("/venues/{slug}/availability")
	@SecurityRequirements
	AvailabilityResponse forPlayers(@PathVariable String slug,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return this.availabilityService.forPlayers(slug, date);
	}

	@GetMapping("/owner/venues/{venueId}/availability")
	AvailabilityResponse forOwner(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return this.availabilityService.forOwner(CurrentUser.id(jwt), venueId, date);
	}

	@PostMapping("/owner/venues/{venueId}/courts/{courtId}/blocks")
	@ResponseStatus(HttpStatus.CREATED)
	CourtBlockResponse createBlock(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID courtId, @Valid @RequestBody CourtBlockRequest request) {
		return this.courtBlockService.create(CurrentUser.id(jwt), venueId, courtId, request);
	}

	@GetMapping("/owner/venues/{venueId}/courts/{courtId}/blocks")
	List<CourtBlockResponse> listBlocks(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID courtId, @RequestParam OffsetDateTime from, @RequestParam OffsetDateTime to) {
		Instant start = from.toInstant();
		Instant end = to.toInstant();
		if (!end.isAfter(start) || end.isAfter(start.plus(Duration.ofDays(92)))) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RANGE", "Use a range of up to 92 days");
		}
		return this.courtBlockService.list(CurrentUser.id(jwt), venueId, courtId, start, end);
	}

	@DeleteMapping("/owner/venues/{venueId}/courts/{courtId}/blocks/{blockId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteBlock(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId, @PathVariable UUID courtId,
			@PathVariable UUID blockId) {
		this.courtBlockService.delete(CurrentUser.id(jwt), venueId, courtId, blockId);
	}

}
