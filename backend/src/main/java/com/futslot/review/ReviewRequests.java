package com.futslot.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

final class ReviewRequests {

	private ReviewRequests() {
	}

	/** {@code comment} is optional: a star rating alone is a review. */
	record Write(@NotNull @Min(1) @Max(5) Integer rating, @Size(max = 1000) String comment) {

	}

	record Reply(@NotBlank @Size(max = 1000) String text) {

	}

	record Moderate(@NotNull ReviewStatus status) {

	}

}
