package com.futslot.venue;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param advancePercent share of the price a player must pay online to confirm (0 = pay at venue)
 * @param bookingHoldMinutes how long an unpaid booking holds the slot
 * @param cancellationTiers fees for late cancellations, ordered from furthest to closest to the start;
 * empty means cancelling is always free
 * @param maxAdvanceBookingDays how far ahead players can book
 */
public record VenuePolicy(@Min(0) @Max(100) int advancePercent, @Min(5) @Max(120) int bookingHoldMinutes,
		@NotNull @Size(max = 6) List<@NotNull @Valid CancellationTier> cancellationTiers,
		@Min(1) @Max(180) int maxAdvanceBookingDays) {

	public VenuePolicy {
		cancellationTiers = (cancellationTiers != null) ? cancellationTiers.stream()
			.sorted(Comparator.comparingInt(CancellationTier::withinHours).reversed())
			.toList() : null;
	}

	/** The fee share for a player cancelling {@code notice} before the start; 0 when it's free. */
	public int feePercent(Duration notice) {
		int fee = 0;
		for (CancellationTier tier : this.cancellationTiers) {
			if (notice.compareTo(Duration.ofHours(tier.withinHours())) < 0) {
				fee = tier.feePercent();
			}
		}
		return fee;
	}

	/** Why the tiers can't be used, or null when they can. */
	String tierProblem() {
		for (int i = 1; i < this.cancellationTiers.size(); i++) {
			CancellationTier further = this.cancellationTiers.get(i - 1);
			CancellationTier closer = this.cancellationTiers.get(i);
			if (closer.withinHours() == further.withinHours()) {
				return "Two cancellation fees start " + closer.withinHours() + " hours before";
			}
			if (closer.feePercent() <= further.feePercent()) {
				return "The fee must go up as the start gets closer: " + closer.feePercent() + "% within "
						+ closer.withinHours() + " hours is not more than " + further.feePercent() + "% within "
						+ further.withinHours() + " hours";
			}
		}
		return null;
	}

}
