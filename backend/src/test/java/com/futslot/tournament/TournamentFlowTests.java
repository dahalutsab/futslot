package com.futslot.tournament;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.futslot.support.IntegrationTest;
import com.futslot.support.TestUsers;
import com.futslot.support.TestUsers.TestUser;
import com.futslot.support.TestVenues;
import com.futslot.support.TestVenues.TestVenue;
import com.jayway.jsonpath.JsonPath;

@IntegrationTest
class TournamentFlowTests {

	private static final LocalDate GAME_DAY = LocalDate.now(ZoneId.of("Asia/Kathmandu")).plusDays(3);

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Test
	void knockoutWithEntryFeesWaitlistAndAByeForTheTopSeed() throws Exception {
		TestUser owner = this.users.signUp("Cup Owner");
		TestVenue venue = this.venues.active(owner, "Cup Futsal");
		String tournamentId = create(owner, venue, "Dashain Cup", "KNOCKOUT", 3, 1000, null);
		String admin = manage(venue, tournamentId);

		Squad tigers = team("Tigers");
		Squad lions = team("Lions");
		Squad bears = team("Bears");
		Squad wolves = team("Wolves");
		register(tigers, tournamentId).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REGISTRATION_CLOSED"));
		send(owner, post(admin + "/open"), null).andExpect(jsonPath("$.status").value("REGISTRATION_OPEN"));

		send(tigers.captain(), post("/api/v1/tournaments/" + tournamentId + "/registrations"), """
				{"teamId":"%s","squad":[{"userId":"%s"},{"userId":"%s"}]}""".formatted(tigers.teamId(), tigers.players().get(0),
				tigers.players().get(1))).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_SQUAD_SIZE"));
		send(tigers.captain(), post("/api/v1/tournaments/" + tournamentId + "/registrations"), """
				{"teamId":"%s","squad":[{"userId":"%s"},{"userId":"%s"},{"userId":"%s"}]}""".formatted(tigers.teamId(),
				tigers.players().get(0), tigers.players().get(1), lions.players().get(0))).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("NOT_TEAM_MEMBERS"));
		String tigersEntry = entryId(register(tigers, tournamentId).andExpect(status().isCreated()), "Tigers");
		String lionsEntry = entryId(register(lions, tournamentId), "Lions");
		String bearsEntry = entryId(register(bears, tournamentId), "Bears");
		String wolvesEntry = entryId(register(wolves, tournamentId), "Wolves");
		send(owner, get(admin), null)
			.andExpect(jsonPath("$.teamsIn").value(3))
			.andExpect(jsonPath("$.waitlisted").value(1))
			.andExpect(jsonPath("$.registrations[3].status").value("WAITLISTED"));
		send(owner, get("/api/v1/notifications"), null).andExpect(jsonPath("$.content[0].type").value("TOURNAMENT_REGISTRATION"));

		// Tigers pay by wallet and staff verify; Lions pay cash at the counter
		send(tigers.captain(), post("/api/v1/tournament-entries/" + tigersEntry + "/payments"),
				"{\"method\":\"ESEWA\",\"transactionId\":\"ES-%s\",\"amount\":1000}".formatted(UUID.randomUUID()))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.purpose").value("TOURNAMENT_ENTRY"));
		send(tigers.captain(), get("/api/v1/tournament-entries/" + tigersEntry + "/payments"), null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].status").value("PENDING"));
		send(lions.captain(), get("/api/v1/tournament-entries/" + tigersEntry + "/payments"), null)
			.andExpect(status().isNotFound());
		String paymentId = JsonPath.read(send(owner, get("/api/v1/owner/venues/" + venue.id() + "/payments/pending"), null)
			.andExpect(jsonPath("$[0].customerName").value("Tigers"))
			.andReturn().getResponse().getContentAsString(), "$[0].id");
		send(owner, post("/api/v1/owner/venues/" + venue.id() + "/payments/" + paymentId + "/approve"), null)
			.andExpect(status().isOk());
		send(tigers.captain(), get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("TOURNAMENT_ENTRY_CONFIRMED"));
		recordFee(owner, venue, lionsEntry).andExpect(status().isCreated());

		// Bears pull out, so Wolves get their place
		send(bears.captain(), post("/api/v1/tournaments/" + tournamentId + "/registrations/" + bearsEntry + "/withdraw"), null)
			.andExpect(status().isOk());
		send(wolves.captain(), get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("TOURNAMENT_PLACE_OFFERED"));
		recordFee(owner, venue, wolvesEntry).andExpect(status().isCreated());

		send(owner, post(admin + "/draw"), null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("CLOSE_REGISTRATION_FIRST"));
		send(owner, post(admin + "/close"), null).andExpect(status().isOk());
		String drawn = send(owner, post(admin + "/draw"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
			.andExpect(jsonPath("$.matches.length()").value(3))
			.andExpect(jsonPath("$.matches[0].stage").value("SEMI_FINAL"))
			.andExpect(jsonPath("$.matches[0].home.name").value("Tigers"))
			.andExpect(jsonPath("$.matches[0].bye").value(true))
			.andExpect(jsonPath("$.matches[1].home.name").value("Lions"))
			.andExpect(jsonPath("$.matches[1].away.name").value("Wolves"))
			.andExpect(jsonPath("$.matches[2].stage").value("FINAL"))
			.andExpect(jsonPath("$.matches[2].home.name").value("Tigers"))
			.andExpect(jsonPath("$.matches[2].away").doesNotExist())
			.andReturn().getResponse().getContentAsString();
		send(lions.captain(), get("/api/v1/notifications"), null).andExpect(jsonPath("$.content[0].type").value("TOURNAMENT_FIXTURES"));
		String semi = JsonPath.read(drawn, "$.matches[1].id");
		String finalMatch = JsonPath.read(drawn, "$.matches[2].id");

		// Scheduling blocks the court
		send(owner, put(admin + "/matches/" + semi + "/schedule"),
				"{\"courtId\":\"%s\",\"startsAt\":\"%sT10:00:00+05:45\"}".formatted(venue.courtId(), GAME_DAY))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.matches[1].court.name").value("Court A"))
			.andExpect(jsonPath("$.matches[1].scheduledAt").value(GAME_DAY + "T10:00:00+05:45"));
		this.mvc.perform(get("/api/v1/venues/" + venue.slug() + "/availability").param("date", GAME_DAY.toString()))
			.andExpect(jsonPath("$.courts[0].slots[4].status").value("BLOCKED"));
		send(this.users.signUp("Would-be Booker"), post("/api/v1/bookings"),
				"{\"courtId\":\"%s\",\"startsAt\":\"%sT10:00:00+05:45\"}".formatted(venue.courtId(), GAME_DAY))
			.andExpect(status().isConflict());
		send(wolves.captain(), get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("TOURNAMENT_MATCH_SCHEDULED"))
			.andExpect(jsonPath("$.content[0].title").value("Lions vs Wolves"));

		send(owner, post(admin + "/matches/" + finalMatch + "/result"), "{\"homeScore\":1,\"awayScore\":0}")
			.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TEAMS_NOT_SET"));
		send(owner, post(admin + "/matches/" + semi + "/result"), "{\"homeScore\":2,\"awayScore\":2}")
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PENALTIES_REQUIRED"));
		send(owner, post(admin + "/matches/" + semi + "/result"),
				"{\"homeScore\":2,\"awayScore\":2,\"homePenalties\":3,\"awayPenalties\":4}")
			.andExpect(jsonPath("$.matches[1].winnerRegistrationId").value(wolvesEntry))
			.andExpect(jsonPath("$.matches[2].away.name").value("Wolves"));
		// A corrected result swaps who goes through
		send(owner, post(admin + "/matches/" + semi + "/result"), "{\"homeScore\":3,\"awayScore\":2}")
			.andExpect(jsonPath("$.matches[2].away.name").value("Lions"));
		send(owner, post(admin + "/matches/" + finalMatch + "/result"), "{\"homeScore\":1,\"awayScore\":0}")
			.andExpect(jsonPath("$.status").value("COMPLETED"))
			.andExpect(jsonPath("$.champion.name").value("Tigers"));
		send(owner, post(admin + "/matches/" + semi + "/result"), "{\"homeScore\":0,\"awayScore\":1}")
			.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("RESULT_LOCKED"));

		String slug = JsonPath.read(drawn, "$.slug");
		this.mvc.perform(get("/api/v1/tournaments/" + slug)).andExpect(status().isOk())
			.andExpect(jsonPath("$.champion.name").value("Tigers"))
			.andExpect(jsonPath("$.registrations[*].team.name", containsInAnyOrder("Tigers", "Lions", "Wolves")))
			.andExpect(jsonPath("$.registrations[0].squad").doesNotExist());
	}

	@Test
	void leagueTableDecidesTheWinner() throws Exception {
		TestUser owner = this.users.signUp("League Owner");
		TestVenue venue = this.venues.active(owner, "League Futsal");
		String tournamentId = create(owner, venue, "Tihar League", "LEAGUE", 4, 0, null);
		String admin = manage(venue, tournamentId);
		send(owner, post(admin + "/open"), null);
		List<Squad> teams = List.of(team("Reds"), team("Blues"), team("Greens"));
		for (Squad team : teams) {
			register(team, tournamentId).andExpect(status().isCreated());
		}
		send(teams.get(0).captain(), get("/api/v1/me/tournament-entries"), null)
			.andExpect(jsonPath("$[0].entry.status").value("CONFIRMED"))
			.andExpect(jsonPath("$[0].entry.squad.length()").value(3));
		send(owner, post(admin + "/close"), null);
		String drawn = send(owner, post(admin + "/draw"), null).andExpect(jsonPath("$.matches.length()").value(3))
			.andExpect(jsonPath("$.tables[0].rows.length()").value(3))
			.andReturn().getResponse().getContentAsString();

		List<String> matches = JsonPath.read(drawn, "$.matches[*].id");
		List<String> homes = JsonPath.read(drawn, "$.matches[*].home.name");
		List<String> aways = JsonPath.read(drawn, "$.matches[*].away.name");
		// Reds beat everyone; Blues and Greens draw
		for (int i = 0; i < 3; i++) {
			String score;
			if (homes.get(i).equals("Reds")) {
				score = "{\"homeScore\":2,\"awayScore\":0}";
			}
			else if (aways.get(i).equals("Reds")) {
				score = "{\"homeScore\":0,\"awayScore\":2}";
			}
			else {
				score = "{\"homeScore\":1,\"awayScore\":1}";
			}
			send(owner, post(admin + "/matches/" + matches.get(i) + "/result"), score).andExpect(status().isOk());
		}
		send(owner, get(admin), null)
			.andExpect(jsonPath("$.status").value("COMPLETED"))
			.andExpect(jsonPath("$.champion.name").value("Reds"))
			.andExpect(jsonPath("$.tables[0].rows[0].team.name").value("Reds"))
			.andExpect(jsonPath("$.tables[0].rows[0].points").value(6))
			.andExpect(jsonPath("$.tables[0].rows[0].goalDifference").value(4))
			.andExpect(jsonPath("$.tables[0].rows[1].points").value(1));
	}

	@Test
	void groupWinnersAndRunnersUpCrossIntoTheSemis() throws Exception {
		TestUser owner = this.users.signUp("Groups Owner");
		TestVenue venue = this.venues.active(owner, "Groups Futsal");
		String tournamentId = create(owner, venue, "Valley Cup", "GROUP_KNOCKOUT", 6, 0, 2);
		String admin = manage(venue, tournamentId);
		send(owner, post(admin + "/open"), null);
		List<String> names = List.of("G1", "G2", "G3", "G4", "G5", "G6");
		for (String name : names) {
			register(team(name), tournamentId).andExpect(status().isCreated());
		}
		send(owner, post(admin + "/close"), null);
		// Seeded by sign-up order and snake-drawn: A = G1, G4, G5; B = G2, G3, G6
		String drawn = send(owner, post(admin + "/draw"), null)
			.andExpect(jsonPath("$.matches.length()").value(6))
			.andExpect(jsonPath("$.tables[0].groupName").value("A"))
			.andExpect(jsonPath("$.tables[0].rows[*].team.name", containsInAnyOrder("G1", "G4", "G5")))
			.andReturn().getResponse().getContentAsString();
		send(owner, post(admin + "/knockout"), null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("GROUPS_NOT_FINISHED"));

		// The team listed first in each group (by seed) wins all its games; G5 beats G4, G6 beats G3
		List<String> ids = JsonPath.read(drawn, "$.matches[*].id");
		List<String> homes = JsonPath.read(drawn, "$.matches[*].home.name");
		List<String> aways = JsonPath.read(drawn, "$.matches[*].away.name");
		List<String> strength = List.of("G1", "G2", "G5", "G6", "G4", "G3");
		for (int i = 0; i < ids.size(); i++) {
			boolean homeWins = strength.indexOf(homes.get(i)) < strength.indexOf(aways.get(i));
			send(owner, post(admin + "/matches/" + ids.get(i) + "/result"),
					homeWins ? "{\"homeScore\":1,\"awayScore\":0}" : "{\"homeScore\":0,\"awayScore\":1}")
				.andExpect(status().isOk());
		}
		send(owner, post(admin + "/knockout"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.matches.length()").value(9))
			.andExpect(jsonPath("$.matches[6].stage").value("SEMI_FINAL"))
			.andExpect(jsonPath("$.matches[6].home.name").value("G1"))
			.andExpect(jsonPath("$.matches[6].away.name").value("G6"))
			.andExpect(jsonPath("$.matches[7].home.name").value("G2"))
			.andExpect(jsonPath("$.matches[7].away.name").value("G5"))
			.andExpect(jsonPath("$.matches[8].stage").value("FINAL"));
	}

	@Test
	void draftsArePrivateAndCancellingFreesTheCourts() throws Exception {
		TestUser owner = this.users.signUp("Private Cup Owner");
		TestVenue venue = this.venues.active(owner, "Private Cup Futsal");
		String tournamentId = create(owner, venue, "Secret Cup", "KNOCKOUT", 2, 0, null);
		String admin = manage(venue, tournamentId);
		String slug = JsonPath.read(send(owner, get(admin), null).andReturn().getResponse().getContentAsString(), "$.slug");
		this.mvc.perform(get("/api/v1/tournaments/" + slug)).andExpect(status().isNotFound());
		send(this.users.signUp("Not Staff"), post("/api/v1/owner/venues/" + venue.id() + "/tournaments"), body("X", "KNOCKOUT", 2, 0, null))
			.andExpect(status().isNotFound());
		send(owner, post("/api/v1/owner/venues/" + venue.id() + "/tournaments"), body("Bad Groups", "GROUP_KNOCKOUT", 4, 0, 2))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_TOURNAMENT"));

		send(owner, post(admin + "/open"), null);
		this.mvc.perform(get("/api/v1/tournaments").param("city", "Kathmandu"))
			.andExpect(jsonPath("$[*].slug", hasItem(slug)));
		Squad home = team("Home Side");
		Squad away = team("Away Side");
		register(home, tournamentId);
		register(away, tournamentId);
		send(owner, post(admin + "/close"), null);
		String match = JsonPath.read(send(owner, post(admin + "/draw"), null).andReturn().getResponse().getContentAsString(),
				"$.matches[0].id");
		send(owner, put(admin + "/matches/" + match + "/schedule"),
				"{\"courtId\":\"%s\",\"startsAt\":\"%sT11:00:00+05:45\"}".formatted(venue.courtId(), GAME_DAY))
			.andExpect(status().isOk());

		send(owner, post(admin + "/cancel"), null).andExpect(jsonPath("$.status").value("CANCELLED"))
			.andExpect(jsonPath("$.matches[0].status").value("CANCELLED"))
			.andExpect(jsonPath("$.matches[0].court").doesNotExist());
		this.mvc.perform(get("/api/v1/venues/" + venue.slug() + "/availability").param("date", GAME_DAY.toString()))
			.andExpect(jsonPath("$.courts[0].slots[5].status").value("AVAILABLE"));
		send(home.captain(), get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[*].type", hasItem("TOURNAMENT_CANCELLED")));
	}

	/** A team of three: a captain and two invited players who accept. */
	private Squad team(String name) throws Exception {
		TestUser captain = this.users.signUp(name + " Captain");
		String teamId = JsonPath.read(send(captain, post("/api/v1/teams"), "{\"name\":\"%s\"}".formatted(name))
			.andReturn().getResponse().getContentAsString(), "$.id");
		List<UUID> players = new ArrayList<>(List.of(captain.id()));
		for (int i = 1; i <= 2; i++) {
			String phone = TestUsers.nextLocalPhone();
			TestUser player = this.users.logIn(phone, name + " Player " + i);
			send(captain, post("/api/v1/teams/" + teamId + "/invitations"), "{\"phone\":\"%s\"}".formatted(phone))
				.andExpect(status().isCreated());
			send(player, post("/api/v1/teams/" + teamId + "/invitation/accept"), null).andExpect(status().isOk());
			players.add(player.id());
		}
		return new Squad(captain, teamId, players);
	}

	private ResultActions register(Squad team, String tournamentId) throws Exception {
		return send(team.captain(), post("/api/v1/tournaments/" + tournamentId + "/registrations"), """
				{"teamId":"%s","squad":[{"userId":"%s","jerseyNumber":1},{"userId":"%s","jerseyNumber":7},{"userId":"%s"}]}"""
			.formatted(team.teamId(), team.players().get(0), team.players().get(1), team.players().get(2)));
	}

	private ResultActions recordFee(TestUser owner, TestVenue venue, String entryId) throws Exception {
		return send(owner, post("/api/v1/owner/venues/" + venue.id() + "/tournament-entries/" + entryId + "/payments"),
				"{\"method\":\"CASH\"}");
	}

	private static String entryId(ResultActions result, String teamName) throws Exception {
		List<String> ids = JsonPath.read(result.andReturn().getResponse().getContentAsString(),
				"$.registrations[?(@.team.name == '" + teamName + "')].id");
		return ids.getFirst();
	}

	private String create(TestUser owner, TestVenue venue, String name, String format, int maxTeams, int fee,
			Integer groups) throws Exception {
		return JsonPath.read(send(owner, post("/api/v1/owner/venues/" + venue.id() + "/tournaments"),
				body(name, format, maxTeams, fee, groups))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("DRAFT"))
			.andReturn().getResponse().getContentAsString(), "$.id");
	}

	private static String body(String name, String format, int maxTeams, int fee, Integer groups) {
		return """
				{"name":"%s","format":"%s","teamSize":3,"maxSquadSize":5,"maxTeams":%d,"entryFee":%d,
				 "startsOn":"%s","endsOn":"%s","matchMinutes":40%s}""".formatted(name, format, maxTeams, fee, GAME_DAY,
				GAME_DAY.plusDays(1), (groups != null) ? ",\"groupCount\":" + groups : "");
	}

	private static String manage(TestVenue venue, String tournamentId) {
		return "/api/v1/owner/venues/" + venue.id() + "/tournaments/" + tournamentId;
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request, String json) throws Exception {
		request.header("Authorization", user.bearer());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request);
	}

	record Squad(TestUser captain, String teamId, List<UUID> players) {

	}

}
