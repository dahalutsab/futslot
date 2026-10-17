package com.futslot.venue;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * A player cancelling less than {@code withinHours} before the start pays {@code feePercent} of the booking
 * price. Cancelling 24 hours or more ahead is always free.
 */
@Embeddable
public record CancellationTier(@Min(1) @Max(24) @Column(nullable = false) int withinHours,
		@Min(1) @Max(100) @Column(nullable = false) int feePercent) {

}
