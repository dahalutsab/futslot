package com.futslot.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @param completedValue sum of booking totals for completed games (what the customer was charged,
 * not necessarily what was collected; payments come separately)
 */
public record CustomerDetailResponse(UUID id, String name, String phone, boolean hasApp, String notes,
		Instant customerSince, Instant lastBookingAt, Stats stats, List<BookingResponse> recentBookings) {

	public record Stats(long total, long upcoming, long completed, long cancelled, long noShows,
			BigDecimal completedValue) {

	}

}
