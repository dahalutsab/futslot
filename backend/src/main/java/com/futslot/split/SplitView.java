package com.futslot.split;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A booking's split as the booker or one of the players sees it. Before the result, {@code outcomes}
 * shows what each side would pay per player for each result, so everyone knows the stakes. Players see
 * the booker's phone to pay them; only the booker sees the players' phones.
 */
public record SplitView(UUID bookingId, String reference, String venueName, String courtName,
		OffsetDateTime startsAt, Person bookedBy, SplitRule rule, Integer losersPercent, BigDecimal courtAmount,
		BigDecimal extrasAmount, String extrasNote, BigDecimal total, String sideAName, String sideBName,
		List<Player> players, MatchResult result, Instant resultRecordedAt, List<Outcome> outcomes,
		BigDecimal collected, BigDecimal outstanding, boolean youAreBooker) {

	public record Person(UUID userId, String name, String phone) {

	}

	/** {@code share} is set once the result is in. */
	public record Player(UUID id, String name, Side side, boolean booker, boolean you, boolean appUser,
			String phone, BigDecimal share, boolean settled, Instant settledAt) {

	}

	/** Roughly what each player on a side pays for this result (shares differ by at most a rupee). */
	public record Outcome(MatchResult result, BigDecimal sideATotal, BigDecimal sideAEach, BigDecimal sideBTotal,
			BigDecimal sideBEach) {

	}

}
