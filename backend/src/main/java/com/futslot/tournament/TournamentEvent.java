package com.futslot.tournament;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Published after tournament activity; {@code recipients} are who should hear about it. */
public record TournamentEvent(Type type, UUID tournamentId, String slug, String tournamentName, String venueName,
		List<UUID> recipients, String teamName, String opponentName, String courtName, OffsetDateTime at) {

	public enum Type {

		/** A team signed up; the venue hears. */
		REGISTERED,
		/** The team's place is confirmed (free entry, or the fee is in). */
		ENTRY_CONFIRMED,
		/** A place opened up and the waitlisted team got it. */
		PROMOTED,
		/** Fixtures are out; every squad player hears. */
		FIXTURES_DRAWN,
		/** A match got a court and time; both squads hear. */
		MATCH_SCHEDULED,
		/** The venue called the tournament off. */
		CANCELLED

	}

}
