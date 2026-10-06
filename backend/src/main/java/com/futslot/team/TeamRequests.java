package com.futslot.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

final class TeamRequests {

	private TeamRequests() {
	}

	/** Players are invited by the phone number they signed up with. */
	record Invite(@NotBlank String phone) {

	}

	/** CAPTAIN hands the captaincy over; the old captain becomes vice-captain. */
	record ChangeRole(@NotNull TeamRole role) {

	}

}
