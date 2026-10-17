package com.futslot.team;

import java.util.UUID;

/**
 * A player as other players see them: short name, what they play and how much. Profile fields are null
 * if they haven't filled them in or keep their profile private. {@code isPublic} is only set on the player's
 * own card, so they can see whether others see their profile.
 */
public record PlayerCard(UUID userId, String name, String avatarUrl, Position position, SkillLevel skillLevel,
		PreferredFoot preferredFoot, String city, String bio, long gamesPlayed, Boolean isPublic) {

}
