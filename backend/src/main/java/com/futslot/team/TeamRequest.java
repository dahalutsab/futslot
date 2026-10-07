package com.futslot.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamRequest(@NotBlank @Size(min = 2, max = 60) String name, @Size(max = 60) String city,
		SkillLevel skillLevel, @Size(max = 500) String logoUrl, Boolean isPublic) {

}
