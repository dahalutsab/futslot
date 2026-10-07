package com.futslot.common;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class Timestamps {

	private Timestamps() {
	}

	/**
	 * The current instant at the precision PostgreSQL stores, so a value set on a new entity reads the same in the
	 * response as it will from the database.
	 */
	public static Instant now() {
		return Instant.now().truncatedTo(ChronoUnit.MICROS);
	}

}
