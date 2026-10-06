package com.futslot.venue;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

/** Opening hours for one day as sent by clients; open/close may be omitted when {@code closed}. */
public record DayHours(@NotNull DayOfWeek day, LocalTime openTime, LocalTime closeTime, Boolean closed) {

	boolean isClosed() {
		return Boolean.TRUE.equals(this.closed);
	}

	static DayHours from(OperatingHours hours) {
		return new DayHours(hours.dayOfWeek(), hours.openTime(), hours.closeTime(), hours.isClosed());
	}

}
