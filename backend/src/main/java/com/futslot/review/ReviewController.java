package com.futslot.review;

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

import com.futslot.booking.BookingResponse;
import com.futslot.common.CurrentUser;
import com.futslot.common.PageResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1")
@Validated
class ReviewController {

	private static final Sort NEWEST = Sort.by(Sort.Order.desc("createdAt"));

	private final ReviewService reviewService;

	ReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	// Public

	@GetMapping("/venues/{slug}/reviews")
	@SecurityRequirements
	PageResponse<ReviewResponse> venueReviews(@PathVariable String slug,
			@RequestParam(defaultValue = "NEWEST") ReviewSort sort, @RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
		return this.reviewService.published(slug, PageRequest.of(page, size, sort.sort()));
	}

	@GetMapping("/venues/{slug}/reviews/summary")
	@SecurityRequirements
	RatingSummaryResponse summary(@PathVariable String slug) {
		return this.reviewService.summary(slug);
	}

	// Player

	@GetMapping("/bookings/{bookingId}/review")
	ReviewResponse myReview(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId) {
		return this.reviewService.forBooking(CurrentUser.id(jwt), bookingId);
	}

	@PostMapping("/bookings/{bookingId}/review")
	@ResponseStatus(HttpStatus.CREATED)
	ReviewResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId,
			@Valid @RequestBody ReviewRequests.Write request) {
		return this.reviewService.create(CurrentUser.id(jwt), bookingId, request);
	}

	@PutMapping("/bookings/{bookingId}/review")
	ReviewResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId,
			@Valid @RequestBody ReviewRequests.Write request) {
		return this.reviewService.update(CurrentUser.id(jwt), bookingId, request);
	}

	@DeleteMapping("/bookings/{bookingId}/review")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId) {
		this.reviewService.delete(CurrentUser.id(jwt), bookingId);
	}

	@GetMapping("/me/reviews")
	PageResponse<ReviewResponse> mine(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
		return this.reviewService.mine(CurrentUser.id(jwt), PageRequest.of(page, size, NEWEST));
	}

	@GetMapping("/me/reviews/pending")
	List<BookingResponse> awaitingReview(@AuthenticationPrincipal Jwt jwt) {
		return this.reviewService.awaitingReview(CurrentUser.id(jwt));
	}

	// Venue

	@GetMapping("/owner/venues/{venueId}/reviews")
	PageResponse<ReviewResponse> forVenue(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@RequestParam(defaultValue = "false") boolean unanswered, @RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return this.reviewService.forVenue(CurrentUser.id(jwt), venueId, unanswered, PageRequest.of(page, size, NEWEST));
	}

	@PutMapping("/owner/venues/{venueId}/reviews/{reviewId}/reply")
	ReviewResponse reply(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId, @PathVariable UUID reviewId,
			@Valid @RequestBody ReviewRequests.Reply request) {
		return this.reviewService.reply(CurrentUser.id(jwt), venueId, reviewId, request.text());
	}

	@DeleteMapping("/owner/venues/{venueId}/reviews/{reviewId}/reply")
	ReviewResponse deleteReply(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID reviewId) {
		return this.reviewService.deleteReply(CurrentUser.id(jwt), venueId, reviewId);
	}

}
