package com.futslot.tournament;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** API shapes for tournaments. */
public final class TournamentViews {

	private TournamentViews() {
	}

	public record Summary(UUID id, String slug, String name, VenueRef venue, TournamentFormat format,
			TournamentStatus status, LocalDate startsOn, LocalDate endsOn, BigDecimal entryFee, int teamsIn,
			int maxTeams, String bannerUrl) {

	}

	/**
	 * Everything on a tournament's page. {@code registrations} lists confirmed teams for the public and every
	 * entry for the venue; {@code tables} holds the league table or one table per group.
	 */
	public record Detail(UUID id, String slug, String name, VenueRef venue, String description, String rules,
			String bannerUrl, TournamentFormat format, TournamentStatus status, int teamSize, int maxSquadSize,
			int maxTeams, BigDecimal entryFee, String prizeDetails, int pointsForWin, int pointsForDraw,
			Instant registrationOpensAt, Instant registrationClosesAt, LocalDate startsOn, LocalDate endsOn,
			int matchMinutes, Integer groupCount, boolean acceptingTeams, int teamsIn, int waitlisted,
			List<Entry> registrations, List<Match> matches, List<Table> tables, TeamRef champion) {

	}

	public record VenueRef(UUID id, String name, String slug, String city) {

	}

	public record TeamRef(UUID registrationId, UUID teamId, String name, String slug) {

	}

	/** {@code squad} is shown to the venue and the team itself. */
	public record Entry(UUID id, TeamRef team, RegistrationStatus status, String groupName, Integer seed,
			List<SquadPlayer> squad, Instant registeredAt) {

	}

	public record SquadPlayer(UUID userId, String name, Integer jerseyNumber) {

	}

	public record Match(UUID id, Stage stage, String groupName, int round, int matchNumber, TeamRef home,
			TeamRef away, CourtRef court, OffsetDateTime scheduledAt, Integer homeScore, Integer awayScore,
			Integer homePenalties, Integer awayPenalties, UUID winnerRegistrationId, MatchStatus status,
			boolean bye) {

	}

	public record CourtRef(UUID id, String name) {

	}

	/** {@code groupName} is null for a league table. */
	public record Table(String groupName, List<Standing> rows) {

	}

	public record Standing(TeamRef team, int played, int won, int drawn, int lost, int goalsFor, int goalsAgainst,
			int goalDifference, int points) {

	}

	/** A player's view of an entry their team made. */
	public record MyEntry(Summary tournament, Entry entry) {

	}

}
