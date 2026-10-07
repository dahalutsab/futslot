package com.futslot.venue;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.futslot.support.IntegrationTest;
import com.futslot.support.TestUsers;
import com.futslot.support.TestUsers.TestUser;
import com.futslot.support.TestVenues;
import com.futslot.support.TestVenues.TestVenue;

@IntegrationTest
class FavouriteTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Test
	void playersSaveAndUnsaveApprovedVenues() throws Exception {
		TestUser owner = this.users.signUp("Fav Owner");
		TestVenue first = this.venues.active(owner, "Fav One");
		TestVenue second = this.venues.active(owner, "Fav Two");
		TestVenue draft = this.venues.draft(owner, "Fav Draft");
		TestUser player = this.users.signUp("Fav Player");

		send(player, put(path(first))).andExpect(status().isNoContent());
		send(player, put(path(second))).andExpect(status().isNoContent());
		send(player, put(path(first))).andExpect(status().isNoContent());
		send(player, put(path(draft))).andExpect(status().isNotFound());

		send(player, get("/api/v1/me/favourites")).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].name", contains("Fav Two", "Fav One")))
			.andExpect(jsonPath("$[0].fromPrice").value(1200.0));
		send(this.users.signUp("Someone Else"), get("/api/v1/me/favourites")).andExpect(jsonPath("$.length()").value(0));

		send(player, delete(path(second))).andExpect(status().isNoContent());
		send(player, delete(path(second))).andExpect(status().isNoContent());
		send(player, get("/api/v1/me/favourites")).andExpect(jsonPath("$[*].name", contains("Fav One")));
		this.mvc.perform(get("/api/v1/me/favourites")).andExpect(status().isUnauthorized());
	}

	private static String path(TestVenue venue) {
		return "/api/v1/me/favourites/" + venue.id();
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request) throws Exception {
		return this.mvc.perform(request.header("Authorization", user.bearer()));
	}

}
