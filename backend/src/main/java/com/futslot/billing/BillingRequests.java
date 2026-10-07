package com.futslot.billing;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

final class BillingRequests {

	private BillingRequests() {
	}

	/** {@code planCode} FREE goes back to the free plan (at the end of any paid period). */
	record ChoosePlan(@NotBlank String planCode, BillingCycle billingCycle) {

	}

	record Feature(@NotNull FeaturedPlacement placement, @NotNull LocalDate startsOn, @NotNull @Min(1) @Max(30) Integer days) {

	}

}
