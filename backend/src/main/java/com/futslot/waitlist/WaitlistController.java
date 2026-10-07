package com.futslot.waitlist;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1")
class WaitlistController {

	private final WaitlistService waitlistService;

	WaitlistController(WaitlistService waitlistService) {
		this.waitlistService = waitlistService;
	}

	/** Wait for a booked slot; you're told if it frees up. */
	@PostMapping("/waitlist")
	@ResponseStatus(HttpStatus.CREATED)
	WaitlistView join(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody WaitlistRequest request) {
		return this.waitlistService.join(CurrentUser.id(jwt), request);
	}

	@GetMapping("/me/waitlist")
	List<WaitlistView> mine(@AuthenticationPrincipal Jwt jwt) {
		return this.waitlistService.mine(CurrentUser.id(jwt));
	}

	@DeleteMapping("/me/waitlist/{entryId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void leave(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID entryId) {
		this.waitlistService.leave(CurrentUser.id(jwt), entryId);
	}

}
