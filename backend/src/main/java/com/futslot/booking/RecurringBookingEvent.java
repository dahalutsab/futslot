package com.futslot.booking;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Published once per series change instead of one {@link BookingEvent} per occurrence.
 * {@code skippedDates} lists weeks that could not be booked (for SKIPPED and CREATED).
 */
public record RecurringBookingEvent(Type type, UUID seriesId, UUID venueId, String venueName, String courtName,
		String customerName, UUID playerUserId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
		LocalDate startsOn, List<LocalDate> skippedDates, UUID actorUserId) {

	public enum Type {

		CREATED, CANCELLED, SKIPPED

	}

	static RecurringBookingEvent of(Type type, RecurringBooking series, List<LocalDate> skippedDates, UUID actorUserId) {
		return new RecurringBookingEvent(type, series.getId(), series.getVenue().getId(), series.getVenue().getName(),
				series.getCourt().getName(), series.getCustomer().getName(), series.getCustomer().getUserId(),
				series.getDayOfWeek(), series.getStartTime(), series.getEndTime(), series.getStartsOn(), skippedDates,
				actorUserId);
	}

}
