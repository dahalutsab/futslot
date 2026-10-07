package com.futslot.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One day of a venue's activity; every day in the period is present, quiet ones as zeros. */
public record DailyRow(LocalDate date, long bookings, BigDecimal bookedHours, BigDecimal bookedValue, long cancelled,
		BigDecimal collected) {

}
