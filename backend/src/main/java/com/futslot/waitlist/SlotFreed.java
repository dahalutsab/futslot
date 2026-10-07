package com.futslot.waitlist;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** A slot people were waiting for is free; {@code recipients} should hurry. */
public record SlotFreed(UUID venueId, String venueName, String venueSlug, UUID courtId, String courtName,
		OffsetDateTime startsAt, List<UUID> recipients) {

}
