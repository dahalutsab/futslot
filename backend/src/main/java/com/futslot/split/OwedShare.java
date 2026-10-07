package com.futslot.split;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Money the viewer owes someone who booked a game they played in. */
public record OwedShare(UUID bookingId, String venueName, OffsetDateTime startsAt, String side, MatchResult result,
		BigDecimal amount, SplitView.Person payTo) {

}
