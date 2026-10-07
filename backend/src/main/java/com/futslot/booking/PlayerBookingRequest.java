package com.futslot.booking;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.futslot.venue.AddOnLine;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Players book exactly one slot, identified by its start time from the availability response. Either
 * {@code membershipId} (one of the player's memberships at this venue) or {@code discountCode} can lower
 * the court price, not both. {@code addOns} are extras from the venue's list (bibs, water).
 */
public record PlayerBookingRequest(@NotNull UUID courtId, @NotNull OffsetDateTime startsAt,
		@Size(max = 500) String notes, UUID membershipId, @Size(max = 32) String discountCode,
		@Size(max = 10) List<@Valid AddOnLine> addOns) {

}
