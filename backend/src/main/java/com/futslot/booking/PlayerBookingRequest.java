package com.futslot.booking;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Players book exactly one slot, identified by its start time from the availability response. */
public record PlayerBookingRequest(@NotNull UUID courtId, @NotNull OffsetDateTime startsAt,
		@Size(max = 500) String notes) {

}
