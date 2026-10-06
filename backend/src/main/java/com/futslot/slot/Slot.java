package com.futslot.slot;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.futslot.venue.PriceLabel;

/** Times are in the venue's local offset (e.g. 2026-11-01T18:00+05:45) so clients can display them as-is. */
public record Slot(OffsetDateTime startsAt, OffsetDateTime endsAt, BigDecimal price, PriceLabel label,
		SlotStatus status) {

	public TimeRange range() {
		return new TimeRange(this.startsAt.toInstant(), this.endsAt.toInstant());
	}

}
