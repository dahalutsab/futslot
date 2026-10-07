package com.futslot.team;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.futslot.support.IntegrationTest;
import com.futslot.support.TestUsers;
import com.futslot.support.TestUsers.TestUser;
import com.jayway.jsonpath.JsonPath;

@IntegrationTest
class TeamFlowTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Test
	void captainBuildsASquadAndHandsOver() throws Exception {
		TestUser captain = this.users.signUp("Captain Kiran");
		String body = send(captain, post("/api/v1/teams"), "{\"name\":\"Patan Panthers\",\"city\":\"Lalitpur\"}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.slug").value("patan-panthers"))
			.andExpect(jsonPath("$.myRole").value("CAPTAIN"))
			.andExpect(jsonPath("$.memberCount").value(1))
			.andReturn().getResponse().getContentAsString();
		String teamId = JsonPath.read(body, "$.id");
		String team = "/api/v1/teams/" + teamId;

		send(captain, post(team + "/invitations"), "{\"phone\":\"9800000999\"}").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PLAYER_NOT_FOUND"));
		String bikashPhone = TestUsers.nextLocalPhone();
		TestUser bikash = this.users.logIn(bikashPhone, "Bikash Gurung");
		send(captain, post(team + "/invitations"), "{\"phone\":\"%s\"}".formatted(bikashPhone)).andExpect(status().isCreated());
		send(bikash, get("/api/v1/notifications"), null).andExpect(jsonPath("$.content[0].type").value("TEAM_INVITE"));
		send(bikash, get("/api/v1/me/teams"), null).andExpect(jsonPath("$[0].myStatus").value("INVITED"));
		send(bikash, post(team + "/invitation/accept"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.myStatus").value("ACTIVE"))
			.andExpect(jsonPath("$.memberCount").value(2));
		send(captain, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("TEAM_MEMBER_JOINED"));
		send(captain, post(team + "/invitations"), "{\"phone\":\"%s\"}".formatted(bikashPhone))
			.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_MEMBER"));

		// Members can't run the team until promoted
		String thirdPhone = TestUsers.nextLocalPhone();
		TestUser third = this.users.logIn(thirdPhone, "Third Player");
		send(bikash, post(team + "/invitations"), "{\"phone\":\"%s\"}".formatted(thirdPhone))
			.andExpect(status().isForbidden());
		send(captain, put(team + "/members/" + bikash.id() + "/role"), "{\"role\":\"VICE_CAPTAIN\"}")
			.andExpect(status().isOk());
		send(bikash, post(team + "/invitations"), "{\"phone\":\"%s\"}".formatted(thirdPhone))
			.andExpect(status().isCreated());
		send(third, post(team + "/invitation/decline"), null).andExpect(status().isNoContent());
		send(captain, get("/api/v1/teams/patan-panthers"), null)
			.andExpect(jsonPath("$.members[*].name", contains("Captain Kiran", "Bikash Gurung")));

		send(captain, post(team + "/leave"), null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("CAPTAIN_CANNOT_LEAVE"));
		send(captain, put(team + "/members/" + bikash.id() + "/role"), "{\"role\":\"CAPTAIN\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.captainId").value(bikash.id().toString()))
			.andExpect(jsonPath("$.myRole").value("VICE_CAPTAIN"));
		send(bikash, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("TEAM_CAPTAIN_CHANGED"));
		send(captain, put(team + "/members/" + bikash.id() + "/role"), "{\"role\":\"MEMBER\"}")
			.andExpect(status().isForbidden());
		send(bikash, delete(team + "/members/" + captain.id()), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.memberCount").value(1));
		send(captain, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("TEAM_MEMBER_REMOVED"));
	}

	@Test
	void privateTeamsStayPrivateAndPublicOnesAreFindable() throws Exception {
		TestUser captain = this.users.signUp("Hidden Captain");
		send(captain, post("/api/v1/teams"), "{\"name\":\"Secret Squad\",\"city\":\"Pokhara\",\"isPublic\":false}")
			.andExpect(status().isCreated());
		send(captain, post("/api/v1/teams"), "{\"name\":\"Lakeside Legends\",\"city\":\"Pokhara\",\"skillLevel\":\"ADVANCED\"}")
			.andExpect(status().isCreated());
		TestUser stranger = this.users.signUp("Curious Stranger");

		send(stranger, get("/api/v1/teams/secret-squad"), null).andExpect(status().isNotFound());
		send(stranger, get("/api/v1/teams").param("city", "pokhara").param("q", "lake"), null)
			.andExpect(jsonPath("$.content[*].name", contains("Lakeside Legends")));
		send(stranger, get("/api/v1/teams").param("city", "Pokhara"), null)
			.andExpect(jsonPath("$.content[*].name", not(hasItem("Secret Squad"))));
		send(stranger, get("/api/v1/teams/lakeside-legends"), null)
			.andExpect(jsonPath("$.members[0].name").value("Hidden C."))
			.andExpect(jsonPath("$.myRole").doesNotExist());
	}

	@Test
	void playersShareAProfileOrKeepItPrivate() throws Exception {
		TestUser player = this.users.signUp("Sunil Thapa");
		send(player, put("/api/v1/me/player-profile"),
				"{\"position\":\"GOALKEEPER\",\"skillLevel\":\"INTERMEDIATE\",\"city\":\"Kathmandu\",\"bio\":\"Safe hands\"}")
			.andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Sunil Thapa"));
		TestUser other = this.users.signUp("Profile Viewer");
		send(other, get("/api/v1/players/" + player.id()), null)
			.andExpect(jsonPath("$.name").value("Sunil T."))
			.andExpect(jsonPath("$.position").value("GOALKEEPER"))
			.andExpect(jsonPath("$.gamesPlayed").value(0));

		send(player, put("/api/v1/me/player-profile"), "{\"position\":\"GOALKEEPER\",\"isPublic\":false}")
			.andExpect(status().isOk());
		send(other, get("/api/v1/players/" + player.id()), null)
			.andExpect(jsonPath("$.name").value("Sunil T."))
			.andExpect(jsonPath("$.position").doesNotExist());
		send(player, get("/api/v1/me/player-profile"), null).andExpect(jsonPath("$.position").value("GOALKEEPER"));
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request, String json) throws Exception {
		request.header("Authorization", user.bearer());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request);
	}

}
