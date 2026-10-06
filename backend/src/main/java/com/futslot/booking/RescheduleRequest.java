package com.futslot.booking;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** {@code courtId} defaults to the current court, {@code endsAt} to the current duration. */
public record RescheduleRequest(UUID courtId, @NotNull OffsetDateTime startsAt, OffsetDateTime endsAt) {

}
