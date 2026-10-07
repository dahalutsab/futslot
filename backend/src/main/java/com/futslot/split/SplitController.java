package com.futslot.split;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;

import jakarta.validation.Valid;

/** Everything but reading is for the booker. */
@RestController
@RequestMapping("/api/v1")
class SplitController {

	private final SplitService splitService;

	SplitController(SplitService splitService) {
		this.splitService = splitService;
	}

	@GetMapping("/bookings/{bookingId}/split")
	SplitView get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId) {
		return this.splitService.get(CurrentUser.id(jwt), bookingId);
	}

	@PutMapping("/bookings/{bookingId}/split")
	SplitView setUp(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId,
			@Valid @RequestBody SplitRequests.Rules request) {
		return this.splitService.setUp(CurrentUser.id(jwt), bookingId, request);
	}

	@PostMapping("/bookings/{bookingId}/split/players")
	SplitView addPlayer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId,
			@Valid @RequestBody SplitRequests.AddPlayer request) {
		return this.splitService.addPlayer(CurrentUser.id(jwt), bookingId, request);
	}

	@PutMapping("/bookings/{bookingId}/split/players/{playerId}")
	SplitView movePlayer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId, @PathVariable UUID playerId,
			@Valid @RequestBody SplitRequests.MovePlayer request) {
		return this.splitService.movePlayer(CurrentUser.id(jwt), bookingId, playerId, request.side());
	}

	@DeleteMapping("/bookings/{bookingId}/split/players/{playerId}")
	SplitView removePlayer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId,
			@PathVariable UUID playerId) {
		return this.splitService.removePlayer(CurrentUser.id(jwt), bookingId, playerId);
	}

	@PostMapping("/bookings/{bookingId}/split/result")
	SplitView recordResult(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId,
			@RequestBody SplitRequests.Result request) {
		return this.splitService.recordResult(CurrentUser.id(jwt), bookingId, request.result());
	}

	@DeleteMapping("/bookings/{bookingId}/split/result")
	SplitView undoResult(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId) {
		return this.splitService.undoResult(CurrentUser.id(jwt), bookingId);
	}

	@PostMapping("/bookings/{bookingId}/split/players/{playerId}/paid")
	SplitView markPaid(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId, @PathVariable UUID playerId) {
		return this.splitService.markPaid(CurrentUser.id(jwt), bookingId, playerId, true);
	}

	@DeleteMapping("/bookings/{bookingId}/split/players/{playerId}/paid")
	SplitView markUnpaid(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId, @PathVariable UUID playerId) {
		return this.splitService.markPaid(CurrentUser.id(jwt), bookingId, playerId, false);
	}

	/** What the viewer still owes from games other people booked. */
	@GetMapping("/me/split-dues")
	List<OwedShare> owed(@AuthenticationPrincipal Jwt jwt) {
		return this.splitService.owed(CurrentUser.id(jwt));
	}

}
