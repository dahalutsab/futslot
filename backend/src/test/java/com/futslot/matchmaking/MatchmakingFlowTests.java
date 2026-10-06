package com.futslot.matchmaking;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;

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
class MatchmakingFlowTests {

	private static final LocalDate GAME_DAY = LocalDate.now(ZoneId.of("Asia/Kathmandu")).plusDays(3);

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Test
	void playersFillAGameAndPlacesReopenWhenSomeoneDropsOut() throws Exception {
		TestVenue venue = this.venues.active(this.users.signUp("Match Owner"), "Match Futsal");
		TestUser host = this.users.signUp("Host Hari");
		String bookingId = id(send(host, post("/api/v1/bookings"),
				"{\"courtId\":\"%s\",\"startsAt\":\"%sT19:00:00+05:45\"}".formatted(venue.courtId(), GAME_DAY)));
		String postId = id(send(host, post("/api/v1/match-posts"), """
				{"type":"NEED_PLAYERS","bookingId":"%s","playersNeeded":2,"costPerPlayer":200,"note":"Friendly 5-a-side"}"""
			.formatted(bookingId))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.venue.name").value("Match Futsal"))
			.andExpect(jsonPath("$.city").value("Kathmandu"))
			.andExpect(jsonPath("$.startsAt").value(GAME_DAY + "T19:00:00+05:45")));
		String path = "/api/v1/match-posts/" + postId;

		TestUser asha = this.users.signUp("Asha Rai");
		TestUser bina = this.users.signUp("Bina Lama");
		TestUser chandra = this.users.signUp("Chandra KC");
		send(asha, get("/api/v1/match-posts").param("city", "kathmandu").param("date", GAME_DAY.toString()), null)
			.andExpect(jsonPath("$.content[*].id", hasItem(postId)));
		send(host, post(path + "/requests"), "{}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("OWN_POST"));
		String ashaRequest = requestId(send(asha, post(path + "/requests"), "{\"message\":\"Can play any position\"}")
			.andExpect(status().isCreated()));
		String binaRequest = requestId(send(bina, post(path + "/requests"), "{}"));
		send(chandra, post(path + "/requests"), "{}").andExpect(status().isCreated());
		send(asha, post(path + "/requests"), "{}").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_REQUESTED"));
		send(host, get("/api/v1/notifications"), null).andExpect(jsonPath("$.content[0].type").value("MATCH_REQUEST"));

		send(host, get(path), null).andExpect(jsonPath("$.requests.length()").value(3))
			.andExpect(jsonPath("$.requests[0].player.name").value("Asha R."))
			.andExpect(jsonPath("$.requests[0].phone").doesNotExist());
		send(asha, get(path), null).andExpect(jsonPath("$.requests").doesNotExist())
			.andExpect(jsonPath("$.postedBy.name").value("Host H."))
			.andExpect(jsonPath("$.contactPhone").doesNotExist());

		send(host, post(path + "/requests/" + ashaRequest + "/accept"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.placesTaken").value(1))
			.andExpect(jsonPath("$.requests[0].phone", startsWith("+977")));
		send(asha, get(path), null).andExpect(jsonPath("$.myRequest.status").value("ACCEPTED"))
			.andExpect(jsonPath("$.contactPhone", startsWith("+977")));
		send(asha, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("MATCH_REQUEST_ACCEPTED"));

		// The last place goes; whoever is still waiting hears no
		send(host, post(path + "/requests/" + binaRequest + "/accept"), null)
			.andExpect(jsonPath("$.status").value("FILLED"));
		send(chandra, get(path), null).andExpect(jsonPath("$.myRequest.status").value("DECLINED"));
		send(chandra, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("MATCH_REQUEST_DECLINED"));

		send(asha, post(path + "/requests/withdraw"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("OPEN"))
			.andExpect(jsonPath("$.placesTaken").value(1));
		send(host, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("MATCH_PLAYER_WITHDREW"));

		send(host, post(path + "/cancel"), null).andExpect(jsonPath("$.status").value("CANCELLED"));
		send(bina, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("MATCH_POST_CANCELLED"));
		send(bina, get("/api/v1/me/match-requests"), null).andExpect(jsonPath("$.content[0].id").value(postId));
	}

	@Test
	void teamsFindOpponents() throws Exception {
		TestUser homeCaptain = this.users.signUp("Home Captain");
		String homeTeam = id(send(homeCaptain, post("/api/v1/teams"), "{\"name\":\"Home Heroes\"}"));
		TestUser awayCaptain = this.users.signUp("Away Captain");
		String awayTeam = id(send(awayCaptain, post("/api/v1/teams"), "{\"name\":\"Away Aces\"}"));
		String when = GAME_DAY + "T18:00:00+05:45";

		send(homeCaptain, post("/api/v1/match-posts"),
				"{\"type\":\"FIND_OPPONENT\",\"city\":\"Bhaktapur\",\"startsAt\":\"%s\"}".formatted(when))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("TEAM_REQUIRED"));
		send(awayCaptain, post("/api/v1/match-posts"),
				"{\"type\":\"FIND_OPPONENT\",\"teamId\":\"%s\",\"city\":\"Bhaktapur\",\"startsAt\":\"%s\"}".formatted(homeTeam, when))
			.andExpect(status().isNotFound());
		String postId = id(send(homeCaptain, post("/api/v1/match-posts"), """
				{"type":"FIND_OPPONENT","teamId":"%s","city":"Bhaktapur","startsAt":"%s","skillLevel":"INTERMEDIATE"}"""
			.formatted(homeTeam, when)).andExpect(status().isCreated()));
		String path = "/api/v1/match-posts/" + postId;

		send(awayCaptain, post(path + "/requests"), "{}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("TEAM_REQUIRED"));
		String requestId = requestId(send(awayCaptain, post(path + "/requests"), "{\"teamId\":\"%s\"}".formatted(awayTeam))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.myRequest.team.name").value("Away Aces")));
		send(homeCaptain, post(path + "/requests/" + requestId + "/accept"), null)
			.andExpect(jsonPath("$.status").value("FILLED"))
			.andExpect(jsonPath("$.requests[0].team.name").value("Away Aces"));
	}

	@Test
	void postsNeedATimeAndPlaceAndAreCapped() throws Exception {
		TestUser player = this.users.signUp("Eager Poster");
		send(player, post("/api/v1/match-posts"), "{\"type\":\"NEED_PLAYERS\",\"playersNeeded\":1,\"startsAt\":\"%sT10:00:00+05:45\"}"
			.formatted(GAME_DAY)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PLACE_REQUIRED"));
		send(player, post("/api/v1/match-posts"), "{\"type\":\"NEED_PLAYERS\",\"playersNeeded\":1,\"city\":\"Kathmandu\",\"startsAt\":\"2025-01-01T10:00:00+05:45\"}")
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_START"));
		send(player, post("/api/v1/match-posts"), "{\"type\":\"NEED_PLAYERS\",\"city\":\"Kathmandu\",\"startsAt\":\"%sT10:00:00+05:45\"}"
			.formatted(GAME_DAY)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PLAYERS_NEEDED_REQUIRED"));
		for (int i = 0; i < MatchmakingService.MAX_OPEN_POSTS; i++) {
			send(player, post("/api/v1/match-posts"), "{\"type\":\"NEED_PLAYERS\",\"playersNeeded\":1,\"city\":\"Kathmandu\",\"startsAt\":\"%sT1%d:00:00+05:45\"}"
				.formatted(GAME_DAY, i)).andExpect(status().isCreated());
		}
		send(player, post("/api/v1/match-posts"), "{\"type\":\"NEED_PLAYERS\",\"playersNeeded\":1,\"city\":\"Kathmandu\",\"startsAt\":\"%sT16:00:00+05:45\"}"
			.formatted(GAME_DAY)).andExpect(status().isTooManyRequests());
	}

	private static String id(ResultActions result) throws Exception {
		return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
	}

	private static String requestId(ResultActions result) throws Exception {
		return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.myRequest.id");
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request, String json) throws Exception {
		request.header("Authorization", user.bearer());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request);
	}

}
