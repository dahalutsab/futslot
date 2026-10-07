package com.futslot.matchmaking;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.futslot.team.PlayerCard;
import com.futslot.team.SkillLevel;

/**
 * A match post as the viewer may see it. Phone numbers are only shared once a request is accepted: the
 * poster sees accepted players' numbers, accepted players see the poster's ({@code contactPhone}).
 * {@code requests} is only filled in for the poster.
 */
public record MatchPostView(UUID id, MatchPostType type, MatchPostStatus status, Person postedBy, TeamRef team,
		VenueRef venue, String city, OffsetDateTime startsAt, Integer playersNeeded, long placesTaken,
		SkillLevel skillLevel, BigDecimal costPerPlayer, String note, boolean mine, MyRequest myRequest,
		String contactPhone, List<RequestView> requests, Instant createdAt) {

	public record Person(UUID userId, String name) {

	}

	public record TeamRef(UUID id, String name, String slug) {

	}

	public record VenueRef(UUID id, String name, String slug) {

	}

	public record MyRequest(UUID id, JoinRequestStatus status, TeamRef team) {

	}

	public record RequestView(UUID id, PlayerCard player, TeamRef team, String message, JoinRequestStatus status,
			String phone, Instant createdAt) {

	}

}
