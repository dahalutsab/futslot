package com.futslot.venue;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Adding staff: {@code phone} and {@code fullName} identify the person (an account is created if they
 * have none). {@code permissions} defaults to the role's defaults.
 */
public record StaffRequest(@NotBlank String phone, @NotBlank @Size(max = 100) String fullName,
		@NotNull StaffRole role, Set<VenuePermission> permissions) {

}
