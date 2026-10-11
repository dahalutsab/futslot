package com.futslot.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @param strikeAdvancePercent what the customer must pay up front because of recent strikes; null when the
 * usual advance applies
 */
public record CustomerDetailResponse(UUID id, String name, String phone, boolean hasApp, String notes,
		Instant customerSince, Instant lastBookingAt, Stats stats, Integer strikeAdvancePercent,
		List<BookingResponse> recentBookings) {

	/**
	 * @param lateCancellations the customer's own cancellations that fell inside a fee tier
	 * @param recentStrikes late cancellations and no-shows within the venue's strike window
	 * @param completedValue sum of booking totals for completed games (what the customer was charged, not
	 * necessarily what was collected; payments come separately)
	 */
	public record Stats(long total, long upcoming, long completed, long cancelled, long lateCancellations,
			long noShows, long recentStrikes, BigDecimal completedValue) {

	}

}
