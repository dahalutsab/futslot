package com.futslot.venue;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

/** {@code permissions} defaults to the role's defaults; {@code active} defaults to true. */
public record StaffUpdateRequest(@NotNull StaffRole role, Set<VenuePermission> permissions, Boolean active) {

}
