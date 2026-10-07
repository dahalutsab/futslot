package com.futslot.tournament;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;

/** Browsing is public; signing up a team needs its captain or a vice-captain. */
@RestController
@RequestMapping("/api/v1")
class TournamentController {

	private final TournamentService tournamentService;

	TournamentController(TournamentService tournamentService) {
		this.tournamentService = tournamentService;
	}

	@GetMapping("/tournaments")
	@SecurityRequirements
	List<TournamentViews.Summary> browse(@RequestParam(required = false) String city,
			@RequestParam(defaultValue = "true") boolean upcoming) {
		return this.tournamentService.browse(city, upcoming);
	}

	/** Signed-in team members also see their own squad. */
	@GetMapping("/tournaments/{slug}")
	@SecurityRequirements
	TournamentViews.Detail get(@AuthenticationPrincipal Jwt jwt, @PathVariable String slug) {
		return this.tournamentService.get((jwt != null) ? CurrentUser.id(jwt) : null, slug);
	}

	@PostMapping("/tournaments/{tournamentId}/registrations")
	@ResponseStatus(HttpStatus.CREATED)
	TournamentViews.Detail register(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tournamentId,
			@Valid @RequestBody TournamentRequests.Register request) {
		return this.tournamentService.register(CurrentUser.id(jwt), tournamentId, request);
	}

	@PostMapping("/tournaments/{tournamentId}/registrations/{registrationId}/withdraw")
	TournamentViews.Detail withdraw(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tournamentId,
			@PathVariable UUID registrationId) {
		return this.tournamentService.withdraw(CurrentUser.id(jwt), tournamentId, registrationId);
	}

	@GetMapping("/me/tournament-entries")
	List<TournamentViews.MyEntry> mine(@AuthenticationPrincipal Jwt jwt) {
		return this.tournamentService.mine(CurrentUser.id(jwt));
	}

}
