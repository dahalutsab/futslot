package com.futslot.tournament;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.ApiException;
import com.futslot.slot.BlockReason;
import com.futslot.slot.CourtBlockService;
import com.futslot.venue.Court;
import com.futslot.venue.CourtRepository;

/**
 * Fixtures and results. Drawing needs registration closed and uses confirmed teams only. Knockout winners
 * move on as soon as a result is in; a result can be corrected until the next round's match is decided.
 * Scheduling a match blocks its court for the tournament's match length.
 */
@Service
class MatchService {

	private final TournamentService tournaments;

	private final RegistrationRepository registrations;

	private final TournamentMatchRepository matches;

	private final CourtRepository courts;

	private final CourtBlockService courtTime;

	MatchService(TournamentService tournaments, RegistrationRepository registrations, TournamentMatchRepository matches,
			CourtRepository courts, CourtBlockService courtTime) {
		this.tournaments = tournaments;
		this.registrations = registrations;
		this.matches = matches;
		this.courts = courts;
		this.courtTime = courtTime;
	}

	@Transactional
	TournamentViews.Detail draw(UUID userId, UUID venueId, UUID tournamentId, boolean shuffle) {
		Tournament tournament = this.tournaments.locked(this.tournaments.managed(userId, venueId, tournamentId));
		if (tournament.getStatus() != TournamentStatus.REGISTRATION_CLOSED) {
			throw TournamentService.conflict("CLOSE_REGISTRATION_FIRST", "Close registration before drawing fixtures");
		}
		List<Registration> teams = new ArrayList<>(this.registrations.findByTournamentIdOrderByCreatedAt(tournamentId)
			.stream()
			.filter(entry -> entry.getStatus() == RegistrationStatus.CONFIRMED)
			.toList());
		int minimum = (tournament.getFormat() == TournamentFormat.GROUP_KNOCKOUT) ? tournament.getGroupCount() * 3 : 2;
		if (teams.size() < minimum) {
			throw TournamentService.conflict("NOT_ENOUGH_TEAMS", "Needs at least " + minimum + " confirmed teams");
		}
		if (shuffle) {
			Collections.shuffle(teams);
		}
		for (int i = 0; i < teams.size(); i++) {
			teams.get(i).setSeed(i + 1);
		}
		switch (tournament.getFormat()) {
			case LEAGUE -> roundRobin(tournament, teams, Stage.LEAGUE, null, 1, 1);
			case KNOCKOUT -> knockout(tournament, Fixtures.seededSlots(teams), 1, 1);
			case GROUP_KNOCKOUT -> groups(tournament, teams);
		}
		tournament.setStatus(TournamentStatus.IN_PROGRESS);
		this.tournaments.publish(TournamentEvent.Type.FIXTURES_DRAWN, tournament, this.tournaments.players(teams), null);
		return this.tournaments.detail(tournament, userId, true);
	}

	/** GROUP_KNOCKOUT: once every group match is decided, the top two of each group go into the knockout. */
	@Transactional
	TournamentViews.Detail advanceToKnockout(UUID userId, UUID venueId, UUID tournamentId) {
		Tournament tournament = this.tournaments.locked(this.tournaments.managed(userId, venueId, tournamentId));
		List<TournamentMatch> fixtures = this.matches.findByTournamentIdOrderByMatchNumber(tournamentId);
		if (tournament.getFormat() != TournamentFormat.GROUP_KNOCKOUT || tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
			throw TournamentService.conflict("INVALID_TOURNAMENT_STATUS", "Only a group stage in progress can advance");
		}
		if (fixtures.stream().anyMatch(match -> match.getStage().isKnockout())) {
			throw TournamentService.conflict("KNOCKOUT_DRAWN", "The knockout is already drawn");
		}
		if (fixtures.stream().anyMatch(match -> !match.getStatus().isDecided() && match.getStatus() != MatchStatus.CANCELLED)) {
			throw TournamentService.conflict("GROUPS_NOT_FINISHED", "Finish every group match first");
		}
		Map<String, List<Registration>> groups = this.registrations.findByTournamentIdOrderByCreatedAt(tournamentId)
			.stream()
			.filter(entry -> entry.getStatus() == RegistrationStatus.CONFIRMED && entry.getGroupName() != null)
			.collect(Collectors.groupingBy(Registration::getGroupName, TreeMap::new, Collectors.toList()));
		List<List<Registration>> topTwo = new ArrayList<>();
		List<Registration> qualified = new ArrayList<>();
		groups.forEach((group, members) -> {
			List<Registration> table = Standings.table(members,
					fixtures.stream().filter(match -> group.equals(match.getGroupName())).toList(),
					tournament.getPointsForWin(), tournament.getPointsForDraw())
				.stream()
				.map(row -> row.team)
				.toList();
			topTwo.add(table.subList(0, 2));
			qualified.addAll(table.subList(0, 2));
		});
		int lastNumber = fixtures.stream().mapToInt(TournamentMatch::getMatchNumber).max().orElse(0);
		int lastRound = fixtures.stream().mapToInt(TournamentMatch::getRound).max().orElse(0);
		knockout(tournament, Fixtures.crossGroups(topTwo), lastNumber + 1, lastRound + 1);
		this.tournaments.publish(TournamentEvent.Type.FIXTURES_DRAWN, tournament, this.tournaments.players(qualified),
				null);
		return this.tournaments.detail(tournament, userId, true);
	}

	@Transactional
	TournamentViews.Detail schedule(UUID userId, UUID venueId, UUID tournamentId, UUID matchId,
			TournamentRequests.Schedule request) {
		Tournament tournament = this.tournaments.managed(userId, venueId, tournamentId);
		TournamentMatch match = playable(tournament, matchId);
		if (match.getStatus().isDecided()) {
			throw TournamentService.conflict("MATCH_DECIDED", "This match has been played");
		}
		Court court = this.courts.findByIdAndVenueId(request.courtId(), venueId)
			.filter(Court::isActive)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COURT_NOT_FOUND", "Court not found"));
		if (match.getCourtBlockId() != null) {
			this.courtTime.release(match.getCourtBlockId());
		}
		Instant start = request.startsAt().toInstant();
		String note = tournament.getName() + ": " + label(match.getHome()) + " vs " + label(match.getAway());
		UUID block = this.courtTime.reserve(court.getId(), start,
				start.plusSeconds(tournament.getMatchMinutes() * 60L), BlockReason.TOURNAMENT, note, userId);
		match.schedule(court, start, block);
		List<Registration> sides = new ArrayList<>();
		if (match.getHome() != null) {
			sides.add(match.getHome());
		}
		if (match.getAway() != null) {
			sides.add(match.getAway());
		}
		ZoneId zone = ZoneId.of(tournament.getVenue().getTimezone());
		this.tournaments.publish(TournamentEvent.Type.MATCH_SCHEDULED, tournament, this.tournaments.players(sides),
				label(match.getHome()), label(match.getAway()), court.getName(), start.atZone(zone).toOffsetDateTime());
		return this.tournaments.detail(tournament, userId, true);
	}

	@Transactional
	TournamentViews.Detail unschedule(UUID userId, UUID venueId, UUID tournamentId, UUID matchId) {
		Tournament tournament = this.tournaments.managed(userId, venueId, tournamentId);
		TournamentMatch match = playable(tournament, matchId);
		if (match.getCourtBlockId() != null) {
			this.courtTime.release(match.getCourtBlockId());
		}
		match.unschedule();
		return this.tournaments.detail(tournament, userId, true);
	}

	@Transactional
	TournamentViews.Detail recordResult(UUID userId, UUID venueId, UUID tournamentId, UUID matchId,
			TournamentRequests.Result result) {
		Tournament tournament = this.tournaments.locked(this.tournaments.managed(userId, venueId, tournamentId));
		TournamentMatch match = decidable(tournament, matchId);
		int home = result.homeScore();
		int away = result.awayScore();
		Registration winner = (home > away) ? match.getHome() : (away > home) ? match.getAway() : null;
		Integer homePenalties = null;
		Integer awayPenalties = null;
		if (match.getStage().isKnockout() && winner == null) {
			if (result.homePenalties() == null || result.awayPenalties() == null
					|| result.homePenalties().equals(result.awayPenalties())) {
				throw TournamentService.badRequest("PENALTIES_REQUIRED", "A knockout match needs a winner: enter the penalties");
			}
			homePenalties = result.homePenalties();
			awayPenalties = result.awayPenalties();
			winner = (homePenalties > awayPenalties) ? match.getHome() : match.getAway();
		}
		settle(tournament, match, home, away, homePenalties, awayPenalties, winner, MatchStatus.COMPLETED);
		return this.tournaments.detail(tournament, userId, true);
	}

	@Transactional
	TournamentViews.Detail walkover(UUID userId, UUID venueId, UUID tournamentId, UUID matchId, UUID winnerId) {
		Tournament tournament = this.tournaments.locked(this.tournaments.managed(userId, venueId, tournamentId));
		TournamentMatch match = decidable(tournament, matchId);
		boolean homeWins = match.getHome().getId().equals(winnerId);
		if (!homeWins && !match.getAway().getId().equals(winnerId)) {
			throw TournamentService.badRequest("NOT_IN_MATCH", "That team isn't playing this match");
		}
		settle(tournament, match, homeWins ? 3 : 0, homeWins ? 0 : 3, null, null,
				homeWins ? match.getHome() : match.getAway(), MatchStatus.WALKOVER);
		return this.tournaments.detail(tournament, userId, true);
	}

	/** Frees every court booked for matches not yet played. */
	@Transactional
	TournamentViews.Detail cancel(UUID userId, UUID venueId, UUID tournamentId) {
		Tournament tournament = this.tournaments.locked(this.tournaments.managed(userId, venueId, tournamentId));
		if (tournament.getStatus() == TournamentStatus.COMPLETED || tournament.getStatus() == TournamentStatus.CANCELLED) {
			throw TournamentService.conflict("INVALID_TOURNAMENT_STATUS", "This tournament is " + tournament.getStatus());
		}
		for (TournamentMatch match : this.matches.findByTournamentIdOrderByMatchNumber(tournamentId)) {
			if (!match.getStatus().isDecided()) {
				if (match.getCourtBlockId() != null) {
					this.courtTime.release(match.getCourtBlockId());
					match.unschedule();
				}
				match.cancel();
			}
		}
		tournament.setStatus(TournamentStatus.CANCELLED);
		List<Registration> entered = this.registrations.findByTournamentIdOrderByCreatedAt(tournamentId)
			.stream()
			.filter(entry -> entry.getStatus().holdsPlace() || entry.getStatus() == RegistrationStatus.WAITLISTED)
			.toList();
		List<UUID> recipients = new ArrayList<>(this.tournaments.players(entered));
		entered.forEach(entry -> recipients.add(entry.getRegisteredBy()));
		this.tournaments.publish(TournamentEvent.Type.CANCELLED, tournament, recipients, null);
		return this.tournaments.detail(tournament, userId, true);
	}

	private void settle(Tournament tournament, TournamentMatch match, int home, int away, Integer homePenalties,
			Integer awayPenalties, Registration winner, MatchStatus status) {
		Registration previousWinner = match.getWinner();
		match.decide(home, away, homePenalties, awayPenalties, winner, status);
		if (match.getStage().isKnockout() && match.getNextMatch() != null) {
			TournamentMatch next = match.getNextMatch();
			if (previousWinner != null && !previousWinner.equals(winner)) {
				if (previousWinner.equals(next.getHome())) {
					next.setHome(null);
				}
				if (previousWinner.equals(next.getAway())) {
					next.setAway(null);
				}
			}
			moveOn(match, winner);
		}
		boolean finished = switch (tournament.getFormat()) {
			case LEAGUE -> this.matches.findByTournamentIdOrderByMatchNumber(tournament.getId())
				.stream()
				.allMatch(fixture -> fixture.getStatus().isDecided() || fixture.getStatus() == MatchStatus.CANCELLED);
			case KNOCKOUT, GROUP_KNOCKOUT -> match.getStage() == Stage.FINAL;
		};
		if (finished) {
			tournament.setStatus(TournamentStatus.COMPLETED);
		}
	}

	/** Puts the winner into the next match: the lower-numbered feeder fills the home side. */
	private void moveOn(TournamentMatch match, Registration winner) {
		TournamentMatch next = match.getNextMatch();
		List<TournamentMatch> feeders = this.matches.findByNextMatchIdOrderByMatchNumber(next.getId());
		if (feeders.getFirst().getId().equals(match.getId())) {
			next.setHome(winner);
		}
		else {
			next.setAway(winner);
		}
	}

	private void roundRobin(Tournament tournament, List<Registration> teams, Stage stage, String group, int firstRound,
			int firstNumber) {
		int number = firstNumber;
		List<List<Fixtures.Pairing<Registration>>> rounds = Fixtures.roundRobin(teams);
		for (int r = 0; r < rounds.size(); r++) {
			for (Fixtures.Pairing<Registration> pairing : rounds.get(r)) {
				this.matches.save(new TournamentMatch(tournament, stage, group, firstRound + r, number++, pairing.home(),
						pairing.away(), null));
			}
		}
	}

	/** Groups are drawn snake-wise; matches are numbered round by round across all groups. */
	private void groups(Tournament tournament, List<Registration> teams) {
		List<List<Registration>> groups = Fixtures.snakeGroups(teams, tournament.getGroupCount());
		List<List<List<Fixtures.Pairing<Registration>>>> schedules = new ArrayList<>();
		for (int g = 0; g < groups.size(); g++) {
			String name = Fixtures.groupName(g);
			groups.get(g).forEach(entry -> entry.setGroupName(name));
			schedules.add(Fixtures.roundRobin(groups.get(g)));
		}
		int rounds = schedules.stream().mapToInt(List::size).max().orElse(0);
		int number = 1;
		for (int r = 0; r < rounds; r++) {
			for (int g = 0; g < schedules.size(); g++) {
				if (r < schedules.get(g).size()) {
					for (Fixtures.Pairing<Registration> pairing : schedules.get(g).get(r)) {
						this.matches.save(new TournamentMatch(tournament, Stage.GROUP, Fixtures.groupName(g), r + 1, number++,
								pairing.home(), pairing.away(), null));
					}
				}
			}
		}
	}

	/**
	 * A full bracket for {@code slots} (a power of two; null = bye). Matches are created final first so each
	 * knows where its winner goes, but numbered in playing order. Byes are decided at once.
	 */
	private void knockout(Tournament tournament, List<Registration> slots, int firstNumber, int firstRound) {
		int size = slots.size();
		int rounds = Integer.numberOfTrailingZeros(size);
		List<List<TournamentMatch>> byRound = new ArrayList<>(Collections.nCopies(rounds + 1, null));
		int[] firstNumberOfRound = new int[rounds + 1];
		int running = firstNumber;
		for (int r = 1; r <= rounds; r++) {
			firstNumberOfRound[r] = running;
			running += size >> r;
		}
		for (int r = rounds; r >= 1; r--) {
			int count = size >> r;
			Stage stage = Fixtures.stageFor(size >> (r - 1));
			List<TournamentMatch> round = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				TournamentMatch next = (r == rounds) ? null : byRound.get(r + 1).get(i / 2);
				Registration home = (r == 1) ? slots.get(2 * i) : null;
				Registration away = (r == 1) ? slots.get(2 * i + 1) : null;
				round.add(this.matches.save(new TournamentMatch(tournament, stage, null, firstRound + r - 1,
						firstNumberOfRound[r] + i, home, away, next)));
			}
			byRound.set(r, round);
		}
		this.matches.flush();
		for (TournamentMatch match : byRound.get(1)) {
			if (match.getAway() == null) {
				match.bye();
				moveOn(match, match.getHome());
			}
		}
	}

	private TournamentMatch playable(Tournament tournament, UUID matchId) {
		if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
			throw TournamentService.conflict("INVALID_TOURNAMENT_STATUS", "This tournament is " + tournament.getStatus());
		}
		TournamentMatch match = this.matches.findByIdAndTournamentId(matchId, tournament.getId())
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MATCH_NOT_FOUND", "Match not found"));
		if (match.isBye() || match.getStatus() == MatchStatus.CANCELLED) {
			throw TournamentService.conflict("NOT_PLAYABLE", "This match isn't played");
		}
		return match;
	}

	/** Both teams known, and (if already decided) the next round's match not decided yet. */
	private TournamentMatch decidable(Tournament tournament, UUID matchId) {
		if (tournament.getStatus() != TournamentStatus.IN_PROGRESS && tournament.getStatus() != TournamentStatus.COMPLETED) {
			throw TournamentService.conflict("INVALID_TOURNAMENT_STATUS", "This tournament is " + tournament.getStatus());
		}
		TournamentMatch match = this.matches.findByIdAndTournamentId(matchId, tournament.getId())
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MATCH_NOT_FOUND", "Match not found"));
		if (match.isBye() || match.getStatus() == MatchStatus.CANCELLED) {
			throw TournamentService.conflict("NOT_PLAYABLE", "This match isn't played");
		}
		if (match.getHome() == null || match.getAway() == null) {
			throw TournamentService.conflict("TEAMS_NOT_SET", "Both teams aren't known yet");
		}
		if (match.getNextMatch() != null && match.getNextMatch().getStatus().isDecided()) {
			throw TournamentService.conflict("RESULT_LOCKED", "The next round has been played, so this result can't change");
		}
		if (tournament.getStatus() == TournamentStatus.COMPLETED && match.getStage() != Stage.FINAL
				&& tournament.getFormat() != TournamentFormat.LEAGUE) {
			throw TournamentService.conflict("RESULT_LOCKED", "The tournament is over");
		}
		return match;
	}

	private static String label(Registration registration) {
		return (registration != null) ? registration.getTeam().getName() : "TBD";
	}

}
