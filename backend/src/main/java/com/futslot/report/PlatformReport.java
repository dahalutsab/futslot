package com.futslot.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** FutSlot-wide numbers for admins. {@code venuesByStatus} is the current count, not the period's. */
public record PlatformReport(LocalDate from, LocalDate to, Map<String, Long> venuesByStatus, long newVenues,
		long newUsers, long bookings, long playedBookings, BigDecimal bookedValue, long venuesWithBookings,
		List<VenueRow> topVenues) {

	public record VenueRow(UUID venueId, String name, String city, long bookings, BigDecimal bookedValue) {

	}

}
