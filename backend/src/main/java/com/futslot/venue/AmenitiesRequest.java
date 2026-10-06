package com.futslot.venue;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

public record AmenitiesRequest(@NotNull Set<String> codes) {

}
