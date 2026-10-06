package com.futslot.slot;

import java.time.Instant;

/** Half-open interval [start, end), matching the database's tstzrange(starts_at, ends_at, '[)'). */
public record TimeRange(Instant start, Instant end) {

	public boolean overlaps(TimeRange other) {
		return this.start.isBefore(other.end) && other.start.isBefore(this.end);
	}

}
