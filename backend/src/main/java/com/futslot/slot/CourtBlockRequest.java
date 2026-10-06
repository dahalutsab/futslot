package com.futslot.slot;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CourtBlockRequest(@NotNull OffsetDateTime startsAt, @NotNull OffsetDateTime endsAt,
		@NotNull BlockReason reason, @Size(max = 500) String note) {

}
