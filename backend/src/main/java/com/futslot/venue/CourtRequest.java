package com.futslot.venue;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CourtRequest(@NotBlank @Size(max = 60) String name, @NotNull CourtFormat format,
		@NotNull CourtSurface surface, Boolean indoor, @Min(15) @Max(240) int slotDurationMinutes, Integer sortOrder,
		Boolean active) {

}
