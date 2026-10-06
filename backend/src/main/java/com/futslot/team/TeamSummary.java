package com.futslot.team;

import java.util.UUID;

public record TeamSummary(UUID id, String name, String slug, String logoUrl, String city, SkillLevel skillLevel,
		long memberCount) {

}
