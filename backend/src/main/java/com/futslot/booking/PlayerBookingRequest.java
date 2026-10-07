package com.futslot.booking;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Players book exactly one slot, identified by its start time from the availability response. Either
 * {@code membershipId} (one of the player's memberships at this venue) or {@code discountCode} can lower
 * the price, not both.
 */
public record PlayerBookingRequest(@NotNull UUID courtId, @NotNull OffsetDateTime startsAt,
		@Size(max = 500) String notes, UUID membershipId, @Size(max = 32) String discountCode) {

}
