package com.futslot.billing;

import java.time.Instant;
import java.time.ZoneId;

public enum BillingCycle {

	MONTHLY, YEARLY;

	/** The end of a period starting at {@code start}, by the calendar in Nepal. */
	Instant after(Instant start) {
		ZoneId zone = ZoneId.of("Asia/Kathmandu");
		return (this == MONTHLY) ? start.atZone(zone).plusMonths(1).toInstant() : start.atZone(zone).plusYears(1).toInstant();
	}

}
