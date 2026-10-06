package com.futslot.venue;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * @param advancePercent share of the price a player must pay online to confirm (0 = pay at venue)
 * @param bookingHoldMinutes how long an unpaid booking holds the slot
 * @param freeCancelHours cancelling at least this many hours before start gets a full refund
 * @param lateCancelRefundPct refund share for later cancellations
 * @param maxAdvanceBookingDays how far ahead players can book
 */
public record VenuePolicy(@Min(0) @Max(100) int advancePercent, @Min(5) @Max(120) int bookingHoldMinutes,
		@Min(0) @Max(168) int freeCancelHours, @Min(0) @Max(100) int lateCancelRefundPct,
		@Min(1) @Max(180) int maxAdvanceBookingDays) {

}
