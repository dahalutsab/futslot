package com.futslot.matchmaking;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;
import com.futslot.common.PageResponse;
import com.futslot.team.SkillLevel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1")
@Validated
class MatchmakingController {

	private final MatchmakingService matchmakingService;

	MatchmakingController(MatchmakingService matchmakingService) {
		this.matchmakingService = matchmakingService;
	}

	/** Open posts, soonest first. {@code date} is a Nepal date; {@code skillLevel} also matches posts open to all. */
	@GetMapping("/match-posts")
	PageResponse<MatchPostView> browse(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String city,
			@RequestParam(required = false) MatchPostType type,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(required = false) SkillLevel skillLevel, @RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
		return this.matchmakingService.browse(CurrentUser.id(jwt), city, type, date, skillLevel,
				PageRequest.of(page, size, Sort.by("startsAt")));
	}

	@PostMapping("/match-posts")
	@ResponseStatus(HttpStatus.CREATED)
	MatchPostView create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MatchPostRequest request) {
		return this.matchmakingService.create(CurrentUser.id(jwt), request);
	}

	@GetMapping("/match-posts/{postId}")
	MatchPostView get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID postId) {
		return this.matchmakingService.get(CurrentUser.id(jwt), postId);
	}

	@PostMapping("/match-posts/{postId}/cancel")
	MatchPostView cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID postId) {
		return this.matchmakingService.cancel(CurrentUser.id(jwt), postId);
	}

	@PostMapping("/match-posts/{postId}/requests")
	@ResponseStatus(HttpStatus.CREATED)
	MatchPostView ask(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID postId,
			@Valid @RequestBody JoinRequestBody body) {
		return this.matchmakingService.ask(CurrentUser.id(jwt), postId, body);
	}

	@PostMapping("/match-posts/{postId}/requests/withdraw")
	MatchPostView withdraw(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID postId) {
		return this.matchmakingService.withdraw(CurrentUser.id(jwt), postId);
	}

	@PostMapping("/match-posts/{postId}/requests/{requestId}/accept")
	MatchPostView accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID postId, @PathVariable UUID requestId) {
		return this.matchmakingService.accept(CurrentUser.id(jwt), postId, requestId);
	}

	@PostMapping("/match-posts/{postId}/requests/{requestId}/decline")
	MatchPostView decline(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID postId, @PathVariable UUID requestId) {
		return this.matchmakingService.decline(CurrentUser.id(jwt), postId, requestId);
	}

	@GetMapping("/me/match-posts")
	PageResponse<MatchPostView> mine(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
		return this.matchmakingService.mine(CurrentUser.id(jwt), PageRequest.of(page, size));
	}

	@GetMapping("/me/match-requests")
	PageResponse<MatchPostView> myRequests(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
		return this.matchmakingService.myRequests(CurrentUser.id(jwt), PageRequest.of(page, size));
	}

}
