package com.futslot.venue;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;

@RestController
@RequestMapping("/api/v1/me/favourites")
class FavouriteController {

	private final FavouriteService favouriteService;

	FavouriteController(FavouriteService favouriteService) {
		this.favouriteService = favouriteService;
	}

	@GetMapping
	List<VenueSummaryResponse> list(@AuthenticationPrincipal Jwt jwt) {
		return this.favouriteService.list(CurrentUser.id(jwt));
	}

	@PutMapping("/{venueId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void add(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		this.favouriteService.add(CurrentUser.id(jwt), venueId);
	}

	@DeleteMapping("/{venueId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		this.favouriteService.remove(CurrentUser.id(jwt), venueId);
	}

}
