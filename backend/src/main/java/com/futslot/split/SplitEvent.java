package com.futslot.split;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** For one player with the app; consumed by notifications after commit. */
public record SplitEvent(Type type, UUID bookingId, UUID recipientUserId, String bookerName, String venueName,
		OffsetDateTime startsAt, String sideName, String winnerName, boolean draw, BigDecimal share) {

	public enum Type {

		/** Added to a booking's game. */
		ADDED,
		/** The result is in and their share is known. */
		RESULT

	}

}
