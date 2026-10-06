package com.futslot.report;

import java.time.DayOfWeek;

/** Played bookings starting in this local weekday and hour; only non-empty cells are returned. */
public record PeakHourCell(DayOfWeek dayOfWeek, int hour, long bookings) {

}
