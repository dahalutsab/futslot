package com.futslot.tournament;

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

/**
 * Running a tournament: draft, open and close registration, draw fixtures, schedule matches and enter
 * results. Needs MANAGE_VENUE.
 */
@RestController
@RequestMapping("/api/v1/owner/venues/{venueId}/tournaments")
class OwnerTournamentController {

	private final TournamentService tournamentService;

	private final MatchService matchService;

	OwnerTournamentController(TournamentService tournamentService, MatchService matchService) {
		this.tournamentService = tournamentService;
		this.matchService = matchService;
	}

	@GetMapping
	List<TournamentViews.Summary> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		return this.tournamentService.forVenue(CurrentUser.id(jwt), venueId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	TournamentViews.Detail create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@Valid @RequestBody TournamentRequest request) {
		return this.tournamentService.create(CurrentUser.id(jwt), venueId, request);
	}

	@GetMapping("/{tournamentId}")
	TournamentViews.Detail get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID tournamentId) {
		return this.tournamentService.forVenue(CurrentUser.id(jwt), venueId, tournamentId);
	}

	@PutMapping("/{tournamentId}")
	TournamentViews.Detail update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID tournamentId, @Valid @RequestBody TournamentRequest request) {
		return this.tournamentService.update(CurrentUser.id(jwt), venueId, tournamentId, request);
	}

	@PostMapping("/{tournamentId}/open")
	TournamentViews.Detail open(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID tournamentId) {
		return this.tournamentService.openRegistration(CurrentUser.id(jwt), venueId, tournamentId);
	}

	@PostMapping("/{tournamentId}/close")
	TournamentViews.Detail close(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID tournamentId) {
		return this.tournamentService.closeRegistration(CurrentUser.id(jwt), venueId, tournamentId);
	}

	@PostMapping("/{tournamentId}/registrations/{registrationId}/reject")
	TournamentViews.Detail reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID tournamentId, @PathVariable UUID registrationId) {
		return this.tournamentService.reject(CurrentUser.id(jwt), venueId, tournamentId, registrationId);
	}

	@PostMapping("/{tournamentId}/draw")
	TournamentViews.Detail draw(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID tournamentId, @RequestBody(required = false) TournamentRequests.Draw request) {
		return this.matchService.draw(CurrentUser.id(jwt), venueId, tournamentId,
				request != null && Boolean.TRUE.equals(request.shuffle()));
	}

	@PostMapping("/{tournamentId}/knockout")
	TournamentViews.Detail advance(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID tournamentId) {
		return this.matchService.advanceToKnockout(CurrentUser.id(jwt), venueId, tournamentId);
	}

	@PostMapping("/{tournamentId}/cancel")
	TournamentViews.Detail cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID tournamentId) {
		return this.matchService.cancel(CurrentUser.id(jwt), venueId, tournamentId);
	}

	@PutMapping("/{tournamentId}/matches/{matchId}/schedule")
	TournamentViews.Detail schedule(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID tournamentId, @PathVariable UUID matchId,
			@Valid @RequestBody TournamentRequests.Schedule request) {
		return this.matchService.schedule(CurrentUser.id(jwt), venueId, tournamentId, matchId, request);
	}

	@DeleteMapping("/{tournamentId}/matches/{matchId}/schedule")
	TournamentViews.Detail unschedule(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID tournamentId, @PathVariable UUID matchId) {
		return this.matchService.unschedule(CurrentUser.id(jwt), venueId, tournamentId, matchId);
	}

	@PostMapping("/{tournamentId}/matches/{matchId}/result")
	TournamentViews.Detail result(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID tournamentId, @PathVariable UUID matchId,
			@Valid @RequestBody TournamentRequests.Result request) {
		return this.matchService.recordResult(CurrentUser.id(jwt), venueId, tournamentId, matchId, request);
	}

	@PostMapping("/{tournamentId}/matches/{matchId}/walkover")
	TournamentViews.Detail walkover(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID tournamentId, @PathVariable UUID matchId,
			@Valid @RequestBody TournamentRequests.Walkover request) {
		return this.matchService.walkover(CurrentUser.id(jwt), venueId, tournamentId, matchId,
				request.winnerRegistrationId());
	}

}
