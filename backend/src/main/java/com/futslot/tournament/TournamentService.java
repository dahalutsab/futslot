package com.futslot.tournament;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.futslot.common.ApiException;
import com.futslot.common.Names;
import com.futslot.common.Slugs;
import com.futslot.team.Team;
import com.futslot.team.TeamService;
import com.futslot.user.User;
import com.futslot.user.UserRepository;
import com.futslot.venue.Venue;
import com.futslot.venue.VenueAccess;
import com.futslot.venue.VenuePermission;
import com.futslot.venue.VenueStatus;

/**
 * Setting up tournaments, team sign-ups and what everyone sees. Fixtures and results live in
 * {@link MatchService}. Venue members with MANAGE_VENUE run tournaments; captains and vice-captains sign
 * their teams up. Entry fees are paid through payments, which confirms the entry.
 */
@Service
public class TournamentService {

	private static final Set<TournamentStatus> PUBLIC = EnumSet.of(TournamentStatus.REGISTRATION_OPEN,
			TournamentStatus.REGISTRATION_CLOSED, TournamentStatus.IN_PROGRESS, TournamentStatus.COMPLETED);

	private static final List<RegistrationStatus> PLACES = List.of(RegistrationStatus.PENDING_PAYMENT,
			RegistrationStatus.CONFIRMED);

	private final TournamentRepository tournaments;

	private final RegistrationRepository registrations;

	private final SquadMemberRepository squads;

	private final TournamentMatchRepository matches;

	private final TeamService teams;

	private final UserRepository users;

	private final VenueAccess access;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	TournamentService(TournamentRepository tournaments, RegistrationRepository registrations,
			SquadMemberRepository squads, TournamentMatchRepository matches, TeamService teams, UserRepository users,
			VenueAccess access, ApplicationEventPublisher events, Clock clock) {
		this.tournaments = tournaments;
		this.registrations = registrations;
		this.squads = squads;
		this.matches = matches;
		this.teams = teams;
		this.users = users;
		this.access = access;
		this.events = events;
		this.clock = clock;
	}

	// Venue: setting up

	@Transactional
	TournamentViews.Detail create(UUID userId, UUID venueId, TournamentRequest request) {
		Venue venue = this.access.require(userId, venueId, VenuePermission.MANAGE_VENUE);
		validate(request);
		String slug = Slugs.unique(request.name(), "tournament", this.tournaments::existsBySlug);
		return detail(this.tournaments.save(new Tournament(venue, userId, slug, request)), userId, true);
	}

	/** Format and team size can't change once teams have signed up; nothing changes once fixtures are out. */
	@Transactional
	TournamentViews.Detail update(UUID userId, UUID venueId, UUID tournamentId, TournamentRequest request) {
		Tournament tournament = managed(userId, venueId, tournamentId);
		validate(request);
		if (tournament.fixturesDrawn() || tournament.getStatus() == TournamentStatus.CANCELLED) {
			throw conflict("TOURNAMENT_LOCKED", "This tournament is " + tournament.getStatus() + " and can't be edited");
		}
		boolean hasEntries = !this.registrations.findByTournamentIdOrderByCreatedAt(tournamentId).isEmpty();
		if (hasEntries && (request.format() != tournament.getFormat() || request.teamSize() != tournament.getTeamSize())) {
			throw conflict("TEAMS_REGISTERED", "Teams have signed up, so the format and team size can't change");
		}
		if (request.maxTeams() < this.registrations.countByTournamentIdAndStatusIn(tournamentId, PLACES)) {
			throw conflict("TOO_MANY_TEAMS", "More teams than that have places already");
		}
		tournament.apply(request);
		return detail(tournament, userId, true);
	}

	@Transactional
	TournamentViews.Detail openRegistration(UUID userId, UUID venueId, UUID tournamentId) {
		Tournament tournament = managed(userId, venueId, tournamentId);
		if (tournament.getStatus() != TournamentStatus.DRAFT && tournament.getStatus() != TournamentStatus.REGISTRATION_CLOSED) {
			throw conflict("INVALID_TOURNAMENT_STATUS", "This tournament is " + tournament.getStatus());
		}
		if (tournament.getVenue().getStatus() != VenueStatus.ACTIVE) {
			throw conflict("VENUE_NOT_LIVE", "The venue must be approved before it can run tournaments");
		}
		tournament.setStatus(TournamentStatus.REGISTRATION_OPEN);
		return detail(tournament, userId, true);
	}

	@Transactional
	TournamentViews.Detail closeRegistration(UUID userId, UUID venueId, UUID tournamentId) {
		Tournament tournament = managed(userId, venueId, tournamentId);
		if (tournament.getStatus() != TournamentStatus.REGISTRATION_OPEN) {
			throw conflict("INVALID_TOURNAMENT_STATUS", "Registration isn't open");
		}
		tournament.setStatus(TournamentStatus.REGISTRATION_CLOSED);
		return detail(tournament, userId, true);
	}

	@Transactional(readOnly = true)
	List<TournamentViews.Summary> forVenue(UUID userId, UUID venueId) {
		this.access.require(userId, venueId, null);
		return this.tournaments.findByVenueIdOrderByStartsOnDesc(venueId).stream().map(this::summary).toList();
	}

	@Transactional(readOnly = true)
	TournamentViews.Detail forVenue(UUID userId, UUID venueId, UUID tournamentId) {
		this.access.require(userId, venueId, null);
		Tournament tournament = this.tournaments.findByIdAndVenueId(tournamentId, venueId)
			.orElseThrow(TournamentService::notFound);
		return detail(tournament, userId, true);
	}

	/** Turning a team away frees its place for the next team on the waitlist. */
	@Transactional
	TournamentViews.Detail reject(UUID userId, UUID venueId, UUID tournamentId, UUID registrationId) {
		Tournament tournament = locked(managed(userId, venueId, tournamentId));
		if (tournament.fixturesDrawn()) {
			throw conflict("FIXTURES_DRAWN", "Fixtures are out; teams can't be removed now");
		}
		Registration registration = this.registrations.findByIdAndTournamentId(registrationId, tournamentId)
			.orElseThrow(TournamentService::registrationNotFound);
		boolean heldPlace = registration.getStatus().holdsPlace();
		registration.setStatus(RegistrationStatus.REJECTED);
		if (heldPlace) {
			promoteFromWaitlist(tournament);
		}
		return detail(tournament, userId, true);
	}

	// Public

	@Transactional(readOnly = true)
	List<TournamentViews.Summary> browse(String city, boolean upcomingOnly) {
		Specification<Tournament> spec = (root, q, cb) -> root.get("status").in(PUBLIC);
		if (StringUtils.hasText(city)) {
			spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("venue").get("city")), city.strip().toLowerCase()));
		}
		if (upcomingOnly) {
			spec = spec.and((root, q, cb) -> cb.notEqual(root.get("status"), TournamentStatus.COMPLETED));
		}
		return this.tournaments.findAll(spec, Sort.by("startsOn"))
			.stream()
			.map(this::summary)
			.toList();
	}

	/** A venue's tournaments that teams can still sign up for or follow, soonest first. */
	@Transactional(readOnly = true)
	public List<TournamentViews.Summary> upcomingAtVenue(UUID venueId) {
		Specification<Tournament> spec = (root, q, cb) -> cb.and(root.get("status").in(PUBLIC),
				cb.notEqual(root.get("status"), TournamentStatus.COMPLETED), cb.equal(root.get("venue").get("id"), venueId));
		return this.tournaments.findAll(spec, Sort.by("startsOn")).stream().map(this::summary).toList();
	}

	/** Drafts are only visible to the venue. */
	@Transactional(readOnly = true)
	TournamentViews.Detail get(UUID userId, String slug) {
		Tournament tournament = this.tournaments.findBySlug(slug).orElseThrow(TournamentService::notFound);
		boolean staff = isStaff(userId, tournament);
		if (!staff && !PUBLIC.contains(tournament.getStatus())) {
			throw notFound();
		}
		return detail(tournament, userId, staff);
	}

	// Teams

	/**
	 * A captain or vice-captain signs the team up with a squad of current members. Teams beyond the
	 * tournament's places go on the waitlist; free tournaments confirm straight away.
	 */
	@Transactional
	TournamentViews.Detail register(UUID userId, UUID tournamentId, TournamentRequests.Register request) {
		Tournament tournament = this.tournaments.lockById(tournamentId).orElseThrow(TournamentService::notFound);
		if (!tournament.acceptsRegistrations(this.clock.instant())) {
			throw conflict("REGISTRATION_CLOSED", "This tournament isn't taking teams right now");
		}
		Team team = this.teams.led(userId, request.teamId());
		Registration registration = this.registrations.findByTournamentIdAndTeamId(tournamentId, team.getId()).orElse(null);
		if (registration != null && registration.getStatus() != RegistrationStatus.WITHDRAWN) {
			throw conflict("ALREADY_REGISTERED", team.getName() + " is already " + registration.getStatus());
		}
		List<User> squad = validSquad(tournament, team, request.squad(), (registration != null) ? registration.getId() : null);
		if (registration == null) {
			registration = new Registration(tournament, team, userId);
		}
		else {
			registration.reRegister(userId);
			this.squads.deleteByRegistrationId(registration.getId());
		}
		boolean full = this.registrations.countByTournamentIdAndStatusIn(tournamentId, PLACES) >= tournament.getMaxTeams();
		registration.setStatus(full ? RegistrationStatus.WAITLISTED : placeStatus(tournament));
		registration = this.registrations.save(registration);
		Map<UUID, Integer> jerseys = new HashMap<>();
		request.squad().forEach(entry -> jerseys.put(entry.userId(), entry.jerseyNumber()));
		for (User player : squad) {
			this.squads.save(new SquadMember(registration.getId(), player, jerseys.get(player.getId())));
		}
		publish(TournamentEvent.Type.REGISTERED, tournament,
				this.access.usersWith(tournament.getVenue().getId(), VenuePermission.MANAGE_VENUE), team.getName());
		if (registration.getStatus() == RegistrationStatus.CONFIRMED) {
			publish(TournamentEvent.Type.ENTRY_CONFIRMED, tournament, List.of(userId), team.getName());
		}
		return detail(tournament, userId, false);
	}

	/** Before fixtures are drawn. Any entry fee already paid is for the team and venue to settle. */
	@Transactional
	TournamentViews.Detail withdraw(UUID userId, UUID tournamentId, UUID registrationId) {
		Tournament tournament = this.tournaments.lockById(tournamentId).orElseThrow(TournamentService::notFound);
		Registration registration = this.registrations.findByIdAndTournamentId(registrationId, tournamentId)
			.orElseThrow(TournamentService::registrationNotFound);
		this.teams.led(userId, registration.getTeam().getId());
		if (tournament.fixturesDrawn()) {
			throw conflict("FIXTURES_DRAWN", "Fixtures are out. Talk to the venue");
		}
		if (registration.getStatus() == RegistrationStatus.WITHDRAWN || registration.getStatus() == RegistrationStatus.REJECTED) {
			throw conflict("NOT_REGISTERED", "This entry is " + registration.getStatus());
		}
		boolean heldPlace = registration.getStatus().holdsPlace();
		registration.setStatus(RegistrationStatus.WITHDRAWN);
		if (heldPlace) {
			promoteFromWaitlist(tournament);
		}
		return detail(tournament, userId, false);
	}

	/** Entries of every team the player is in. */
	@Transactional(readOnly = true)
	List<TournamentViews.MyEntry> mine(UUID userId) {
		return this.registrations.findForPlayer(userId)
			.stream()
			.map(registration -> new TournamentViews.MyEntry(summary(registration.getTournament()),
					entry(registration, true)))
			.toList();
	}

	// For payments

	/** An entry waiting for its fee that {@code userId} may pay for (they lead the team). */
	@Transactional(readOnly = true)
	public Registration entryToPay(UUID userId, UUID registrationId) {
		Registration registration = this.registrations.findById(registrationId)
			.orElseThrow(TournamentService::registrationNotFound);
		this.teams.led(userId, registration.getTeam().getId());
		requireFeeDue(registration);
		return registration;
	}

	/** An entry of a team {@code userId} plays for, to show them how paying its fee is going. */
	@Transactional(readOnly = true)
	public Registration entryForPlayer(UUID userId, UUID registrationId) {
		return this.registrations.findById(registrationId)
			.filter(found -> this.teams.squad(found.getTeam().getId()).contains(userId))
			.orElseThrow(TournamentService::registrationNotFound);
	}

	/** An entry at the venue waiting for its fee, for staff recording it. */
	@Transactional(readOnly = true)
	public Registration entryAtVenue(UUID venueId, UUID registrationId) {
		Registration registration = this.registrations.findById(registrationId)
			.filter(found -> found.getTournament().getVenue().getId().equals(venueId))
			.orElseThrow(TournamentService::registrationNotFound);
		requireFeeDue(registration);
		return registration;
	}

	/** The fee is in: confirms the entry and tells the team. */
	@Transactional
	public void feePaid(Registration registration) {
		registration.confirm();
		publish(TournamentEvent.Type.ENTRY_CONFIRMED, registration.getTournament(),
				List.of(registration.getRegisteredBy()), registration.getTeam().getName());
	}

	// Views

	TournamentViews.Detail detail(Tournament tournament, UUID viewerId, boolean staff) {
		List<Registration> entries = this.registrations.findByTournamentIdOrderByCreatedAt(tournament.getId());
		Set<UUID> viewerTeams = new HashSet<>();
		if (viewerId != null) {
			for (Registration entry : entries) {
				if (this.teams.squad(entry.getTeam().getId()).contains(viewerId)) {
					viewerTeams.add(entry.getId());
				}
			}
		}
		List<TournamentViews.Entry> shown = entries.stream()
			.filter(entry -> staff || entry.getStatus() == RegistrationStatus.CONFIRMED || viewerTeams.contains(entry.getId()))
			.map(entry -> entry(entry, staff || viewerTeams.contains(entry.getId())))
			.toList();
		List<TournamentMatch> fixtures = this.matches.findByTournamentIdOrderByMatchNumber(tournament.getId());
		ZoneId zone = ZoneId.of(tournament.getVenue().getTimezone());
		List<TournamentViews.Match> matchViews = fixtures.stream().map(match -> match(match, zone)).toList();
		long teamsIn = entries.stream().filter(entry -> entry.getStatus().holdsPlace()).count();
		long waitlisted = entries.stream().filter(entry -> entry.getStatus() == RegistrationStatus.WAITLISTED).count();
		return new TournamentViews.Detail(tournament.getId(), tournament.getSlug(), tournament.getName(),
				venueRef(tournament.getVenue()), tournament.getDescription(), tournament.getRules(),
				tournament.getBannerUrl(), tournament.getFormat(), tournament.getStatus(), tournament.getTeamSize(),
				tournament.getMaxSquadSize(), tournament.getMaxTeams(), tournament.getEntryFee(),
				tournament.getPrizeDetails(), tournament.getPointsForWin(), tournament.getPointsForDraw(),
				tournament.getRegistrationOpensAt(), tournament.getRegistrationClosesAt(), tournament.getStartsOn(),
				tournament.getEndsOn(), tournament.getMatchMinutes(), tournament.getGroupCount(),
				tournament.acceptsRegistrations(this.clock.instant()), (int) teamsIn, (int) waitlisted, shown,
				matchViews, tables(tournament, entries, fixtures), champion(tournament, fixtures));
	}

	private List<TournamentViews.Table> tables(Tournament tournament, List<Registration> entries,
			List<TournamentMatch> fixtures) {
		List<TournamentViews.Table> tables = new ArrayList<>();
		if (fixtures.isEmpty() || tournament.getFormat() == TournamentFormat.KNOCKOUT) {
			return tables;
		}
		List<Registration> playing = entries.stream().filter(entry -> entry.getStatus() == RegistrationStatus.CONFIRMED).toList();
		if (tournament.getFormat() == TournamentFormat.LEAGUE) {
			tables.add(new TournamentViews.Table(null, rows(tournament, playing, fixtures)));
			return tables;
		}
		Map<String, List<Registration>> groups = playing.stream()
			.filter(entry -> entry.getGroupName() != null)
			.collect(Collectors.groupingBy(Registration::getGroupName, TreeMap::new, Collectors.toList()));
		groups.forEach((group, members) -> tables.add(new TournamentViews.Table(group, rows(tournament, members,
				fixtures.stream().filter(match -> group.equals(match.getGroupName())).toList()))));
		return tables;
	}

	private static List<TournamentViews.Standing> rows(Tournament tournament, List<Registration> teams,
			List<TournamentMatch> fixtures) {
		return Standings.table(teams, fixtures, tournament.getPointsForWin(), tournament.getPointsForDraw())
			.stream()
			.map(row -> new TournamentViews.Standing(teamRef(row.team), row.played, row.won, row.drawn, row.lost,
					row.goalsFor, row.goalsAgainst, row.goalDifference(), row.points))
			.toList();
	}

	private TournamentViews.TeamRef champion(Tournament tournament, List<TournamentMatch> fixtures) {
		if (tournament.getStatus() != TournamentStatus.COMPLETED) {
			return null;
		}
		if (tournament.getFormat() == TournamentFormat.LEAGUE) {
			List<Registration> playing = this.registrations.findByTournamentIdOrderByCreatedAt(tournament.getId())
				.stream()
				.filter(entry -> entry.getStatus() == RegistrationStatus.CONFIRMED)
				.toList();
			List<Standings.Row> table = Standings.table(playing, fixtures, tournament.getPointsForWin(),
					tournament.getPointsForDraw());
			return table.isEmpty() ? null : teamRef(table.getFirst().team);
		}
		return fixtures.stream()
			.filter(match -> match.getStage() == Stage.FINAL && match.getWinner() != null)
			.map(match -> teamRef(match.getWinner()))
			.findFirst()
			.orElse(null);
	}

	TournamentViews.Summary summary(Tournament tournament) {
		int teamsIn = (int) this.registrations.countByTournamentIdAndStatusIn(tournament.getId(), PLACES);
		return new TournamentViews.Summary(tournament.getId(), tournament.getSlug(), tournament.getName(),
				venueRef(tournament.getVenue()), tournament.getFormat(), tournament.getStatus(), tournament.getStartsOn(),
				tournament.getEndsOn(), tournament.getEntryFee(), teamsIn, tournament.getMaxTeams(),
				tournament.getBannerUrl());
	}

	private TournamentViews.Entry entry(Registration registration, boolean withSquad) {
		List<TournamentViews.SquadPlayer> squad = withSquad ? this.squads
			.findByRegistrationIdOrderByJerseyNumber(registration.getId())
			.stream()
			.map(member -> new TournamentViews.SquadPlayer(member.getUserId(), Names.shortName(member.getUser().getFullName()),
					member.getJerseyNumber()))
			.toList() : null;
		return new TournamentViews.Entry(registration.getId(), teamRef(registration), registration.getStatus(),
				registration.getGroupName(), registration.getSeed(), squad, registration.getCreatedAt());
	}

	static TournamentViews.Match match(TournamentMatch match, ZoneId zone) {
		return new TournamentViews.Match(match.getId(), match.getStage(), match.getGroupName(), match.getRound(),
				match.getMatchNumber(), (match.getHome() != null) ? teamRef(match.getHome()) : null,
				(match.getAway() != null) ? teamRef(match.getAway()) : null,
				(match.getCourt() != null) ? new TournamentViews.CourtRef(match.getCourt().getId(), match.getCourt().getName())
						: null,
				(match.getScheduledAt() != null) ? match.getScheduledAt().atZone(zone).toOffsetDateTime() : null,
				match.getHomeScore(), match.getAwayScore(), match.getHomePenalties(), match.getAwayPenalties(),
				(match.getWinner() != null) ? match.getWinner().getId() : null, match.getStatus(), match.isBye());
	}

	static TournamentViews.TeamRef teamRef(Registration registration) {
		Team team = registration.getTeam();
		return new TournamentViews.TeamRef(registration.getId(), team.getId(), team.getName(), team.getSlug());
	}

	private static TournamentViews.VenueRef venueRef(Venue venue) {
		return new TournamentViews.VenueRef(venue.getId(), venue.getName(), venue.getSlug(), venue.getCity());
	}

	// Internals shared with MatchService

	/** A tournament of this venue that {@code userId} may run. */
	Tournament managed(UUID userId, UUID venueId, UUID tournamentId) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_VENUE);
		return this.tournaments.findByIdAndVenueId(tournamentId, venueId).orElseThrow(TournamentService::notFound);
	}

	Tournament locked(Tournament tournament) {
		return this.tournaments.lockById(tournament.getId()).orElseThrow();
	}

	void publish(TournamentEvent.Type type, Tournament tournament, List<UUID> recipients, String teamName) {
		publish(type, tournament, recipients, teamName, null, null, null);
	}

	void publish(TournamentEvent.Type type, Tournament tournament, List<UUID> recipients, String teamName,
			String opponentName, String courtName, OffsetDateTime at) {
		if (!recipients.isEmpty()) {
			this.events.publishEvent(new TournamentEvent(type, tournament.getId(), tournament.getSlug(),
					tournament.getName(), tournament.getVenue().getName(), List.copyOf(new HashSet<>(recipients)),
					teamName, opponentName, courtName, at));
		}
	}

	/** Every player named in these entries' squads. */
	List<UUID> players(List<Registration> entries) {
		if (entries.isEmpty()) {
			return List.of();
		}
		return this.squads.playersOf(entries.stream().map(Registration::getId).toList()).stream().distinct().toList();
	}

	private void promoteFromWaitlist(Tournament tournament) {
		List<Registration> waiting = this.registrations.waitlist(tournament.getId());
		if (waiting.isEmpty()) {
			return;
		}
		Registration next = waiting.getFirst();
		next.setStatus(placeStatus(tournament));
		publish(TournamentEvent.Type.PROMOTED, tournament, List.of(next.getRegisteredBy()), next.getTeam().getName());
	}

	private static RegistrationStatus placeStatus(Tournament tournament) {
		return (tournament.getEntryFee().signum() > 0) ? RegistrationStatus.PENDING_PAYMENT : RegistrationStatus.CONFIRMED;
	}

	private List<User> validSquad(Tournament tournament, Team team, List<TournamentRequests.SquadEntry> entries,
			UUID registrationId) {
		Set<UUID> ids = new HashSet<>();
		Set<Integer> jerseys = new HashSet<>();
		for (TournamentRequests.SquadEntry entry : entries) {
			if (!ids.add(entry.userId())) {
				throw badRequest("INVALID_SQUAD", "A player is listed twice");
			}
			if (entry.jerseyNumber() != null && !jerseys.add(entry.jerseyNumber())) {
				throw badRequest("INVALID_SQUAD", "Jersey " + entry.jerseyNumber() + " is used twice");
			}
		}
		if (ids.size() < tournament.getTeamSize() || ids.size() > tournament.getMaxSquadSize()) {
			throw badRequest("INVALID_SQUAD_SIZE",
					"Name " + tournament.getTeamSize() + " to " + tournament.getMaxSquadSize() + " players");
		}
		if (!new HashSet<>(this.teams.squad(team.getId())).containsAll(ids)) {
			throw badRequest("NOT_TEAM_MEMBERS", "Everyone in the squad must be a current member of " + team.getName());
		}
		List<UUID> taken = this.squads.alreadyInOtherSquads(tournament.getId(),
				(registrationId != null) ? registrationId : new UUID(0, 0), List.copyOf(ids));
		if (!taken.isEmpty()) {
			throw conflict("PLAYER_IN_OTHER_SQUAD", "A player in this squad is already playing for another team here");
		}
		Map<UUID, User> byId = this.users.findAllById(ids).stream().collect(Collectors.toMap(User::getId, Function.identity()));
		return entries.stream().map(entry -> byId.get(entry.userId())).toList();
	}

	private boolean isStaff(UUID userId, Tournament tournament) {
		return userId != null && this.access.memberships(userId)
			.stream()
			.anyMatch(membership -> membership.venue().getId().equals(tournament.getVenue().getId()));
	}

	private static void requireFeeDue(Registration registration) {
		if (registration.getStatus() != RegistrationStatus.PENDING_PAYMENT) {
			throw conflict("NO_FEE_DUE", "This entry is " + registration.getStatus());
		}
	}

	private static void validate(TournamentRequest request) {
		if (request.maxSquadSize() < request.teamSize()) {
			throw badRequest("INVALID_TOURNAMENT", "The squad can't be smaller than the team");
		}
		if (request.endsOn().isBefore(request.startsOn())) {
			throw badRequest("INVALID_TOURNAMENT", "It can't end before it starts");
		}
		if (request.registrationOpensAt() != null && request.registrationClosesAt() != null
				&& !request.registrationClosesAt().isAfter(request.registrationOpensAt())) {
			throw badRequest("INVALID_TOURNAMENT", "Registration must close after it opens");
		}
		switch (request.format()) {
			case LEAGUE -> {
				if (request.maxTeams() > 20) {
					throw badRequest("INVALID_TOURNAMENT", "A league can have at most 20 teams");
				}
			}
			case GROUP_KNOCKOUT -> {
				if (request.groupCount() == null || !Set.of(2, 4, 8).contains(request.groupCount())) {
					throw badRequest("INVALID_TOURNAMENT", "Choose 2, 4 or 8 groups");
				}
				if (request.maxTeams() < request.groupCount() * 3) {
					throw badRequest("INVALID_TOURNAMENT", "Allow at least 3 teams per group");
				}
			}
			case KNOCKOUT -> {
			}
		}
	}

	static ApiException notFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "TOURNAMENT_NOT_FOUND", "Tournament not found");
	}

	private static ApiException registrationNotFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "REGISTRATION_NOT_FOUND", "Entry not found");
	}

	static ApiException conflict(String code, String message) {
		return new ApiException(HttpStatus.CONFLICT, code, message);
	}

	static ApiException badRequest(String code, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, code, message);
	}

}
