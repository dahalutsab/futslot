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
 * @param strikeLimit late cancellations and no-shows within {@link #STRIKE_WINDOW} after which a player pays
 * {@code strikeAdvancePercent} up front; both null when the venue doesn't use strikes
 * @param strikeAdvancePercent the advance for players at the strike limit, if higher than the usual one
 */
public record VenuePolicy(@Min(0) @Max(100) int advancePercent, @Min(5) @Max(120) int bookingHoldMinutes,
		@NotNull @Size(max = 6) List<@NotNull @Valid CancellationTier> cancellationTiers,
		@Min(1) @Max(180) int maxAdvanceBookingDays, @Min(1) @Max(10) Integer strikeLimit,
		@Min(1) @Max(100) Integer strikeAdvancePercent) {

	/** How far back late cancellations and no-shows count as strikes. */
	public static final Duration STRIKE_WINDOW = Duration.ofDays(90);

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

	/** The share a player with {@code strikes} recent strikes pays up front. */
	public int advancePercentFor(long strikes) {
		boolean struck = this.strikeLimit != null && strikes >= this.strikeLimit;
		return struck ? Math.max(this.advancePercent, this.strikeAdvancePercent) : this.advancePercent;
	}

	/** Why the policy can't be used, or null when it can. */
	String problem() {
		if ((this.strikeLimit == null) != (this.strikeAdvancePercent == null)) {
			return "Set both the number of strikes and the advance they require, or neither";
		}
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
