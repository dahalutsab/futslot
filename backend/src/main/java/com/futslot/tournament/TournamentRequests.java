package com.futslot.tournament;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

final class TournamentRequests {

	private TournamentRequests() {
	}

	/** A team signing up with the players it names for the tournament. */
	record Register(@NotNull UUID teamId, @NotEmpty @Size(max = 20) List<@Valid SquadEntry> squad) {

	}

	record SquadEntry(@NotNull UUID userId, @Min(0) @Max(99) Integer jerseyNumber) {

	}

	/** {@code shuffle} draws at random; otherwise teams are seeded in the order they registered. */
	record Draw(Boolean shuffle) {

	}

	record Schedule(@NotNull UUID courtId, @NotNull OffsetDateTime startsAt) {

	}

	/** Penalties only for a knockout match that ended level. */
	record Result(@NotNull @Min(0) @Max(99) Integer homeScore, @NotNull @Min(0) @Max(99) Integer awayScore,
			@Min(0) @Max(99) Integer homePenalties, @Min(0) @Max(99) Integer awayPenalties) {

	}

	/** The other team didn't turn up: recorded as 3-0. */
	record Walkover(@NotNull UUID winnerRegistrationId) {

	}

}
