package com.futslot.booking;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * {@code upcoming}: the next generated occurrences. {@code skipped}: dates this request could not book
 * because the court was taken or blocked (only filled when creating or resuming).
 */
public record RecurringBookingResponse(UUID id, BookingResponse.CourtRef court, BookingResponse.CustomerRef customer,
		DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, LocalDate startsOn, LocalDate endsOn,
		BigDecimal price, RecurringStatus status, LocalDate generatedUntil, List<BookingResponse> upcoming,
		List<Skipped> skipped) {

	public record Skipped(LocalDate date, String reason) {

	}

}
