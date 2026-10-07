package com.futslot.venue;

import java.util.UUID;

/** Published when an admin changes a venue's status; {@code reason} is set for rejections and suspensions. */
public record VenueModerated(UUID venueId, String venueName, UUID ownerId, VenueStatus status, String reason,
		UUID adminId) {

	static VenueModerated of(Venue venue, UUID adminId) {
		return new VenueModerated(venue.getId(), venue.getName(), venue.getOwnerId(), venue.getStatus(),
				venue.getRejectionReason(), adminId);
	}

}
