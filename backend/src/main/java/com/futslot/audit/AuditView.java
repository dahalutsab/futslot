package com.futslot.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** {@code ipAddress} is only shown to FutSlot admins. */
public record AuditView(UUID id, UUID actorId, String actorName, UUID venueId, String action, String entityType,
		UUID entityId, Map<String, String> changes, String ipAddress, Instant createdAt) {

}
