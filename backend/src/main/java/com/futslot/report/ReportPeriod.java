package com.futslot.report;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpStatus;

import com.futslot.common.ApiException;

/**
 * Local dates {@code from} to {@code to}, both included, in {@code zone}. Defaults to the last 30 days up to
 * today.
 */
record ReportPeriod(LocalDate from, LocalDate to, ZoneId zone) {

	static final int MAX_DAYS = 366;

	static ReportPeriod of(LocalDate from, LocalDate to, ZoneId zone, Clock clock) {
		LocalDate end = (to != null) ? to : LocalDate.now(clock.withZone(zone));
		LocalDate start = (from != null) ? from : end.minusDays(29);
		if (end.isBefore(start)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", "'to' must be on or after 'from'");
		}
		ReportPeriod period = new ReportPeriod(start, end, zone);
		if (period.days() > MAX_DAYS) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", "Reports cover at most " + MAX_DAYS + " days");
		}
		return period;
	}

	long days() {
		return ChronoUnit.DAYS.between(this.from, this.to) + 1;
	}

	Instant start() {
		return this.from.atStartOfDay(this.zone).toInstant();
	}

	Instant end() {
		return this.to.plusDays(1).atStartOfDay(this.zone).toInstant();
	}

	/** JDBC wants an offset date-time for timestamptz parameters. */
	OffsetDateTime startParam() {
		return start().atOffset(ZoneOffset.UTC);
	}

	OffsetDateTime endParam() {
		return end().atOffset(ZoneOffset.UTC);
	}

}
