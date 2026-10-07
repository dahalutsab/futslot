package com.futslot.waitlist;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WaitlistView(UUID id, UUID venueId, String venueName, String venueSlug, UUID courtId, String courtName,
		OffsetDateTime startsAt, OffsetDateTime endsAt, WaitlistStatus status, Instant notifiedAt, Instant createdAt) {

}
