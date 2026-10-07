package com.futslot.booking;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
class BookingSearchTests {

	private static final LocalDate DAY = LocalDate.now(ZoneId.of("Asia/Kathmandu")).plusDays(2);

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Test
	void staffFindBookingsByReferenceNameOrPhoneAcrossDays() throws Exception {
		TestUser owner = this.users.signUp("Search Owner");
		TestVenue venue = this.venues.active(owner, "Search Futsal");
		String reference = JsonPath.read(walkIn(owner, venue, DAY, "07:00", "Hari Bahadur", "9811111111"), "$.reference");
		walkIn(owner, venue, DAY.plusDays(1), "08:00", "Harish Rai", "9822222222");
		walkIn(owner, venue, DAY, "09:00", "Sita 50%_off", "9833333333");
		String search = "/api/v1/owner/venues/" + venue.id() + "/bookings/search";

		send(owner, get(search).param("q", reference.toLowerCase())).andExpect(jsonPath("$[*].reference", contains(reference)));
		send(owner, get(search).param("q", "hari")).andExpect(jsonPath("$[*].customer.name",
				containsInAnyOrder("Hari Bahadur", "Harish Rai")));
		send(owner, get(search).param("q", "98222")).andExpect(jsonPath("$[*].customer.name", contains("Harish Rai")));
		send(owner, get(search).param("q", "50%")).andExpect(jsonPath("$[*].customer.name", contains("Sita 50%_off")));
		send(owner, get(search).param("q", "h")).andExpect(jsonPath("$.length()").value(0));
		send(this.users.signUp("Search Snoop"), get(search).param("q", "hari")).andExpect(status().isNotFound());
	}

	private String walkIn(TestUser owner, TestVenue venue, LocalDate day, String time, String name, String phone)
			throws Exception {
		return send(owner, post("/api/v1/owner/venues/" + venue.id() + "/bookings").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"courtId":"%s","startsAt":"%sT%s:00+05:45","customerName":"%s","customerPhone":"%s","source":"WALK_IN"}"""
				.formatted(venue.courtId(), day, time, name, phone)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request) throws Exception {
		return this.mvc.perform(request.header("Authorization", user.bearer()));
	}

}
