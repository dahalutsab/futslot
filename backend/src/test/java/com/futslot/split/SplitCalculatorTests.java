package com.futslot.split;

import static com.futslot.split.Side.A;
import static com.futslot.split.Side.B;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class SplitCalculatorTests {

	private static final List<Side> FIVE_A_SIDE = List.of(A, A, A, A, A, B, B, B, B, B);

	@Test
	void winnerPaysNoneLosersPayAll() {
		assertThat(shares(SplitRule.LOSERS_PAY_ALL, null, 1200, 0, MatchResult.A_WON, FIVE_A_SIDE))
			.containsExactly(0, 0, 0, 0, 0, 240, 240, 240, 240, 240);
	}

	@Test
	void losersSeventyWinnersThirty() {
		assertThat(shares(SplitRule.LOSERS_PAY_PERCENT, 70, 1200, 0, MatchResult.B_WON, FIVE_A_SIDE))
			.containsExactly(168, 168, 168, 168, 168, 72, 72, 72, 72, 72);
	}

	@Test
	void losersPayCourtWinnersPayWater() {
		assertThat(shares(SplitRule.LOSERS_PAY_COURT, null, 1200, 300, MatchResult.B_WON, FIVE_A_SIDE))
			.containsExactly(240, 240, 240, 240, 240, 60, 60, 60, 60, 60);
	}

	@Test
	void drawsAndEqualSplitsShareEverything() {
		assertThat(shares(SplitRule.LOSERS_PAY_ALL, null, 1200, 300, MatchResult.DRAW, FIVE_A_SIDE))
			.containsOnly(150);
		assertThat(shares(SplitRule.EQUAL, null, 1200, 0, MatchResult.A_WON, FIVE_A_SIDE)).containsOnly(120);
	}

	@Test
	void unevenSidesSplitWithinEachSide() {
		assertThat(shares(SplitRule.LOSERS_PAY_ALL, null, 1200, 0, MatchResult.A_WON, List.of(A, A, A, B, B, B, B, B, B)))
			.containsExactly(0, 0, 0, 200, 200, 200, 200, 200, 200);
	}

	@Test
	void oddRupeesGoToTheFirstPlayersAndSharesAlwaysAddUp() {
		assertThat(SplitCalculator.divide(new BigDecimal("1000"), 3))
			.containsExactly(new BigDecimal("334.00"), new BigDecimal("333.00"), new BigDecimal("333.00"));
		assertThat(SplitCalculator.divide(new BigDecimal("1000.50"), 3))
			.containsExactly(new BigDecimal("334.50"), new BigDecimal("333.00"), new BigDecimal("333.00"));
		assertThat(SplitCalculator.divide(new BigDecimal("1801"), 7).stream().reduce(BigDecimal.ZERO, BigDecimal::add))
			.isEqualByComparingTo("1801");
		assertThat(SplitCalculator.divide(new BigDecimal("1801"), 7)).allSatisfy(
				share -> assertThat(share).isBetween(new BigDecimal("257"), new BigDecimal("258")));
	}

	private static List<Integer> shares(SplitRule rule, Integer percent, int court, int extras, MatchResult result,
			List<Side> sides) {
		return SplitCalculator
			.shares(rule, percent, BigDecimal.valueOf(court), BigDecimal.valueOf(extras), result, sides)
			.stream()
			.map(BigDecimal::intValueExact)
			.toList();
	}

}
