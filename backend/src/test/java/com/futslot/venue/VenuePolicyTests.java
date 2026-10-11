package com.futslot.venue;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class VenuePolicyTests {

	private static VenuePolicy tiers(CancellationTier... tiers) {
		return new VenuePolicy(30, 15, List.of(tiers), 30);
	}

	@Test
	void tiersAreOrderedFromFurthestToClosest() {
		VenuePolicy policy = tiers(new CancellationTier(10, 60), new CancellationTier(24, 20),
				new CancellationTier(12, 50));
		assertThat(policy.cancellationTiers()).extracting(CancellationTier::withinHours).containsExactly(24, 12, 10);
	}

	@Test
	void theClosestTierThatAppliesSetsTheFee() {
		VenuePolicy policy = tiers(new CancellationTier(24, 20), new CancellationTier(12, 50),
				new CancellationTier(10, 60));
		assertThat(policy.feePercent(Duration.ofHours(30))).isZero();
		assertThat(policy.feePercent(Duration.ofHours(24))).isZero();
		assertThat(policy.feePercent(Duration.ofHours(23))).isEqualTo(20);
		assertThat(policy.feePercent(Duration.ofHours(12))).isEqualTo(20);
		assertThat(policy.feePercent(Duration.ofHours(11))).isEqualTo(50);
		assertThat(policy.feePercent(Duration.ofMinutes(30))).isEqualTo(60);
	}

	@Test
	void noTiersMeansCancellingIsFree() {
		assertThat(tiers().feePercent(Duration.ofMinutes(5))).isZero();
		assertThat(tiers().tierProblem()).isNull();
	}

	@Test
	void feesMustRiseAsTheStartGetsCloser() {
		assertThat(tiers(new CancellationTier(24, 20), new CancellationTier(12, 50)).tierProblem()).isNull();
		assertThat(tiers(new CancellationTier(24, 50), new CancellationTier(12, 50)).tierProblem())
			.contains("must go up");
		assertThat(tiers(new CancellationTier(24, 60), new CancellationTier(12, 50)).tierProblem())
			.contains("must go up");
		assertThat(tiers(new CancellationTier(12, 20), new CancellationTier(12, 50)).tierProblem())
			.contains("Two cancellation fees");
	}

}
