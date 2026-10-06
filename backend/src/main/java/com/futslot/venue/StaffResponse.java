package com.futslot.venue;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record StaffResponse(UUID id, UUID userId, String fullName, String phone, StaffRole role,
		Set<VenuePermission> permissions, boolean active, Instant createdAt) {

}
