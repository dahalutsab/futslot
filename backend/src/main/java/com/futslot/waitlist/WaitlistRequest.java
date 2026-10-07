package com.futslot.waitlist;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** The slot, as shown in availability. */
public record WaitlistRequest(@NotNull UUID courtId, @NotNull OffsetDateTime startsAt) {

}
