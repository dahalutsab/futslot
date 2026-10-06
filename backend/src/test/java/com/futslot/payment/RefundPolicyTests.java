package com.futslot.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class RefundPolicyTests {

	private static final Instant START = Instant.parse("2026-11-07T13:15:00Z");

	@Test
	void venueCancellationAlwaysRefundsEverything() {
		assertThat(RefundPolicy.refundPercent(false, START.minusSeconds(60), START, 24, 0)).isEqualTo(100);
	}

	@Test
	void playerCancellingInTimeGetsEverythingBack() {
		assertThat(RefundPolicy.refundPercent(true, START.minus(Duration.ofHours(30)), START, 24, 0)).isEqualTo(100);
		assertThat(RefundPolicy.refundPercent(true, START.minus(Duration.ofHours(24)), START, 24, 0)).isEqualTo(100);
	}

	@Test
	void playerCancellingLateGetsTheLateShare() {
		assertThat(RefundPolicy.refundPercent(true, START.minus(Duration.ofHours(23)), START, 24, 50)).isEqualTo(50);
		assertThat(RefundPolicy.refundPercent(true, START.minus(Duration.ofHours(1)), START, 24, 0)).isZero();
	}

}
