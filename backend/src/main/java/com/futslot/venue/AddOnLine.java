package com.futslot.venue;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** "2 x Water (1L)" on a booking. */
public record AddOnLine(@NotNull UUID itemId, @NotNull @Min(1) @Max(50) Integer quantity) {

}
