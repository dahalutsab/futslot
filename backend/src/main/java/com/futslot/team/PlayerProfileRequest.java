package com.futslot.team;

import jakarta.validation.constraints.Size;

/** Every field is optional; saving replaces the whole profile. */
public record PlayerProfileRequest(Position position, SkillLevel skillLevel, PreferredFoot preferredFoot,
		@Size(max = 60) String city, @Size(max = 500) String bio, Boolean isPublic) {

}
