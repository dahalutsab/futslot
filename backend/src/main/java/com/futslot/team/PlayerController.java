package com.futslot.team;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
class PlayerController {

	private final PlayerService playerService;

	PlayerController(PlayerService playerService) {
		this.playerService = playerService;
	}

	@GetMapping("/me/player-profile")
	PlayerCard mine(@AuthenticationPrincipal Jwt jwt) {
		return this.playerService.mine(CurrentUser.id(jwt));
	}

	@PutMapping("/me/player-profile")
	PlayerCard save(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlayerProfileRequest request) {
		return this.playerService.save(CurrentUser.id(jwt), request);
	}

	@GetMapping("/players/{userId}")
	PlayerCard player(@PathVariable UUID userId) {
		return this.playerService.card(userId);
	}

}
