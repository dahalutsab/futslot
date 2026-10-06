package com.futslot.review;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
@Validated
class AdminReviewController {

	private final ReviewService reviewService;

	AdminReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@GetMapping
	PageResponse<ReviewResponse> list(@RequestParam(defaultValue = "PUBLISHED") ReviewStatus status,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return this.reviewService.byStatus(status, PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"))));
	}

	/** Publish, hide or remove a review; the venue's rating counts published reviews only. */
	@PostMapping("/{reviewId}/status")
	ReviewResponse moderate(@PathVariable UUID reviewId, @Valid @RequestBody ReviewRequests.Moderate request) {
		return this.reviewService.moderate(reviewId, request.status());
	}

}
