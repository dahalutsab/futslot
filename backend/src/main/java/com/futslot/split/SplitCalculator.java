package com.futslot.split;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Works out who pays what. Shares are whole rupees where possible; the few rupees (and any paisa) left over
 * go one by one to the players listed first, so the shares always add up to exactly the total. The booker
 * is listed first, so any odd rupee lands with them.
 */
final class SplitCalculator {

	private SplitCalculator() {
	}

	/** Each player's share, in the order of {@code sides} (one entry per player). */
	static List<BigDecimal> shares(SplitRule rule, Integer losersPercent, BigDecimal court, BigDecimal extras,
			MatchResult result, List<Side> sides) {
		BigDecimal total = court.add(extras);
		Side loser = (rule == SplitRule.EQUAL || result == null) ? null : result.loser();
		if (loser == null) {
			return divide(total, sides.size());
		}
		BigDecimal losers = switch (rule) {
			case LOSERS_PAY_ALL -> total;
			case LOSERS_PAY_PERCENT -> total.multiply(BigDecimal.valueOf(losersPercent))
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
			case LOSERS_PAY_COURT -> court;
			case EQUAL -> throw new IllegalStateException();
		};
		BigDecimal winners = total.subtract(losers);
		List<BigDecimal> loserShares = divide(losers, count(sides, loser));
		List<BigDecimal> winnerShares = divide(winners, count(sides, loser.other()));
		List<BigDecimal> shares = new ArrayList<>();
		int nextLoser = 0;
		int nextWinner = 0;
		for (Side side : sides) {
			shares.add((side == loser) ? loserShares.get(nextLoser++) : winnerShares.get(nextWinner++));
		}
		return shares;
	}

	/** {@code amount} split {@code n} ways in whole rupees, the remainder handed out from the first. */
	static List<BigDecimal> divide(BigDecimal amount, int n) {
		List<BigDecimal> shares = new ArrayList<>();
		if (n == 0) {
			return shares;
		}
		long paisa = amount.movePointRight(2).longValueExact();
		long base = (paisa / n) / 100 * 100;
		long remainder = paisa - base * n;
		long[] each = new long[n];
		for (int i = 0; i < n; i++) {
			each[i] = base;
		}
		for (int i = 0; remainder >= 100; i++, remainder -= 100) {
			each[i] += 100;
		}
		each[0] += remainder;
		for (long share : each) {
			shares.add(BigDecimal.valueOf(share, 2));
		}
		return shares;
	}

	static int count(List<Side> sides, Side side) {
		return (int) sides.stream().filter(candidate -> candidate == side).count();
	}

}
