package com.futslot.venue;

import java.util.UUID;

public record StaffAdded(UUID venueId, String venueName, UUID userId, StaffRole role, UUID addedBy) {

}
