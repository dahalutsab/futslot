package com.futslot.team;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;
import com.futslot.common.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1")
@Validated
class TeamController {

	private final TeamService teamService;

	TeamController(TeamService teamService) {
		this.teamService = teamService;
	}

	@GetMapping("/teams")
	PageResponse<TeamSummary> search(@RequestParam(required = false) String city,
			@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
		return this.teamService.search(city, q, PageRequest.of(page, size, Sort.by("name")));
	}

	@GetMapping("/teams/{slug}")
	TeamResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable String slug) {
		return this.teamService.get(CurrentUser.id(jwt), slug);
	}

	@GetMapping("/me/teams")
	List<TeamResponse> mine(@AuthenticationPrincipal Jwt jwt) {
		return this.teamService.mine(CurrentUser.id(jwt));
	}

	@PostMapping("/teams")
	@ResponseStatus(HttpStatus.CREATED)
	TeamResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TeamRequest request) {
		return this.teamService.create(CurrentUser.id(jwt), request);
	}

	@PutMapping("/teams/{teamId}")
	TeamResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID teamId,
			@Valid @RequestBody TeamRequest request) {
		return this.teamService.update(CurrentUser.id(jwt), teamId, request);
	}

	@PostMapping("/teams/{teamId}/invitations")
	@ResponseStatus(HttpStatus.CREATED)
	TeamResponse invite(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID teamId,
			@Valid @RequestBody TeamRequests.Invite request) {
		return this.teamService.invite(CurrentUser.id(jwt), teamId, request.phone());
	}

	@PostMapping("/teams/{teamId}/invitation/accept")
	TeamResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID teamId) {
		return this.teamService.acceptInvite(CurrentUser.id(jwt), teamId);
	}

	@PostMapping("/teams/{teamId}/invitation/decline")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void decline(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID teamId) {
		this.teamService.declineInvite(CurrentUser.id(jwt), teamId);
	}

	@PostMapping("/teams/{teamId}/leave")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void leave(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID teamId) {
		this.teamService.leave(CurrentUser.id(jwt), teamId);
	}

	@DeleteMapping("/teams/{teamId}/members/{userId}")
	TeamResponse remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID teamId, @PathVariable UUID userId) {
		return this.teamService.removeMember(CurrentUser.id(jwt), teamId, userId);
	}

	@PutMapping("/teams/{teamId}/members/{userId}/role")
	TeamResponse changeRole(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID teamId, @PathVariable UUID userId,
			@Valid @RequestBody TeamRequests.ChangeRole request) {
		return this.teamService.changeRole(CurrentUser.id(jwt), teamId, userId, request.role());
	}

}
