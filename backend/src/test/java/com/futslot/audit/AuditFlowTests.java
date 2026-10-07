package com.futslot.audit;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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
class AuditFlowTests {

	private static final LocalDate GAME_DAY = LocalDate.now(ZoneId.of("Asia/Kathmandu")).plusDays(3);

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Test
	void ownersSeeWhoDidWhatAtTheirVenue() throws Exception {
		TestUser owner = this.users.signUp("Audit Owner");
		TestVenue venue = this.venues.active(owner, "Audit Futsal");
		String venuePath = "/api/v1/owner/venues/" + venue.id();
		String managerPhone = TestUsers.nextLocalPhone();
		send(owner, post(venuePath + "/staff"),
				"{\"phone\":\"%s\",\"fullName\":\"Audit Manager\",\"role\":\"MANAGER\"}".formatted(managerPhone))
			.andExpect(status().isCreated());
		TestUser manager = this.users.logIn(managerPhone, "Audit Manager");

		String walkIn = JsonPath.read(send(manager, post(venuePath + "/bookings"), """
				{"courtId":"%s","startsAt":"%sT09:00:00+05:45","customerName":"Walk In","customerPhone":"%s","source":"WALK_IN"}"""
			.formatted(venue.courtId(), GAME_DAY, TestUsers.nextLocalPhone())).andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString(), "$.id");
		send(manager, post(venuePath + "/bookings/" + walkIn + "/payments"), "{\"method\":\"CASH\",\"amount\":1200}")
			.andExpect(status().isCreated());
		send(manager, post(venuePath + "/bookings/" + walkIn + "/cancel"), "{\"reason\":\"Customer called\"}")
			.andExpect(status().isOk());
		send(this.users.signUp("App Player"), post("/api/v1/bookings"),
				"{\"courtId\":\"%s\",\"startsAt\":\"%sT10:00:00+05:45\"}".formatted(venue.courtId(), GAME_DAY))
			.andExpect(status().isCreated());

		send(owner, get(venuePath + "/audit-log"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].action").value("BOOKING_CANCELLED"))
			.andExpect(jsonPath("$.content[0].actorName").value("Audit Manager"))
			.andExpect(jsonPath("$.content[0].changes.reason").value("Customer called"))
			.andExpect(jsonPath("$.content[0].ipAddress").doesNotExist())
			.andExpect(jsonPath("$.content[1].action").value("PAYMENT_RECEIVED"))
			.andExpect(jsonPath("$.content[1].changes.amount").value("1200"))
			.andExpect(jsonPath("$.content[1].changes.method").value("CASH"))
			.andExpect(jsonPath("$.content[2].action").value("BOOKING_CREATED"))
			.andExpect(jsonPath("$.content[2].changes.source").value("WALK_IN"))
			.andExpect(jsonPath("$.content[3].action").value("STAFF_ADDED"))
			.andExpect(jsonPath("$.content[3].actorName").value("Audit Owner"))
			.andExpect(jsonPath("$.content[*].action", hasItem("VENUE_ACTIVE")))
			.andExpect(jsonPath("$.content[*].changes.reference", not(hasItem("APP"))));
		send(owner, get(venuePath + "/audit-log"), null).andExpect(jsonPath("$.totalElements").value(5));

		send(manager, get(venuePath + "/audit-log"), null).andExpect(status().isForbidden());
		send(this.users.signUpAdmin("Audit Admin"), get("/api/v1/admin/audit-log").param("venueId", venue.id().toString()), null)
			.andExpect(jsonPath("$.content[0].ipAddress").value("127.0.0.1"))
			.andExpect(jsonPath("$.content[4].action").value("VENUE_ACTIVE"))
			.andExpect(jsonPath("$.content[4].actorName").value("Approver"));
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request, String json) throws Exception {
		request.header("Authorization", user.bearer());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request);
	}

}
