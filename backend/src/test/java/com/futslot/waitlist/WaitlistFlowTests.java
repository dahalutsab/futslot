package com.futslot.waitlist;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
class WaitlistFlowTests {

	private static final LocalDate GAME_DAY = LocalDate.now(ZoneId.of("Asia/Kathmandu")).plusDays(3);

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void everyoneWaitingHearsWhenTheSlotFreesAndTheFirstToBookGetsIt() throws Exception {
		TestVenue venue = this.venues.active(this.users.signUp("Waitlist Owner"), "Waitlist Futsal");
		TestUser holder = this.users.signUp("Slot Holder");
		String held = id(book(holder, venue, "19:00"));
		TestUser keen = this.users.signUp("Keen Player");
		TestUser patient = this.users.signUp("Patient Player");

		send(keen, post("/api/v1/waitlist"), slot(venue, "20:00")).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SLOT_AVAILABLE"));
		send(holder, post("/api/v1/waitlist"), slot(venue, "19:00")).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SLOT_IS_YOURS"));
		send(keen, post("/api/v1/waitlist"), slot(venue, "19:00")).andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("WAITING"))
			.andExpect(jsonPath("$.courtName").value("Court A"));
		send(keen, post("/api/v1/waitlist"), slot(venue, "19:00")).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_WAITING"));
		send(patient, post("/api/v1/waitlist"), slot(venue, "19:00")).andExpect(status().isCreated());

		send(holder, post("/api/v1/bookings/" + held + "/cancel"), "{}").andExpect(status().isOk());
		for (TestUser waiting : new TestUser[] { keen, patient }) {
			send(waiting, get("/api/v1/notifications"), null)
				.andExpect(jsonPath("$.content[0].type").value("WAITLIST_SLOT_FREE"));
		}
		send(keen, get("/api/v1/me/waitlist"), null).andExpect(jsonPath("$[0].status").value("NOTIFIED"));

		String keenBooking = id(book(keen, venue, "19:00").andExpect(status().isCreated()));
		send(keen, get("/api/v1/me/waitlist"), null).andExpect(jsonPath("$[0].status").value("BOOKED"));
		send(patient, get("/api/v1/me/waitlist"), null).andExpect(jsonPath("$[0].status").value("NOTIFIED"));

		// It frees again: whoever is still listed hears again
		send(keen, post("/api/v1/bookings/" + keenBooking + "/cancel"), "{}");
		send(patient, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("WAITLIST_SLOT_FREE"))
			.andExpect(jsonPath("$.content[1].type").value("WAITLIST_SLOT_FREE"));
		send(keen, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value(not("WAITLIST_SLOT_FREE")));
	}

	@Test
	void aLapsedHoldFreesTheSlotForTheWaitlist() throws Exception {
		TestUser owner = this.users.signUp("Lapse Waitlist Owner");
		TestVenue venue = this.venues.active(owner, "Lapse Waitlist Futsal");
		send(owner, put("/api/v1/owner/venues/" + venue.id() + "/policy"), """
				{"advancePercent":30,"bookingHoldMinutes":15,"cancellationTiers":[{"withinHours":24,"feePercent":100}],"maxAdvanceBookingDays":30}""");
		String held = id(book(this.users.signUp("Slow Payer"), venue, "18:00"));
		TestUser waiting = this.users.signUp("Lapse Waiter");
		String entry = id(send(waiting, post("/api/v1/waitlist"), slot(venue, "18:00")).andExpect(status().isCreated()));

		// The hold runs out; the next write on the court expires it
		this.jdbc.update("UPDATE bookings SET hold_expires_at = now() - interval '1 second' WHERE id = ?::uuid", held);
		book(this.users.signUp("Morning Player"), venue, "07:00").andExpect(status().isCreated());
		send(waiting, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("WAITLIST_SLOT_FREE"));

		send(waiting, delete("/api/v1/me/waitlist/" + entry), null).andExpect(status().isNoContent());
		send(waiting, get("/api/v1/me/waitlist"), null).andExpect(jsonPath("$[0].status").value("CANCELLED"))
			.andExpect(jsonPath("$[*].status", not(hasItem("WAITING"))));
	}

	private ResultActions book(TestUser player, TestVenue venue, String time) throws Exception {
		return send(player, post("/api/v1/bookings"), slot(venue, time));
	}

	private static String slot(TestVenue venue, String time) {
		return "{\"courtId\":\"%s\",\"startsAt\":\"%sT%s:00+05:45\"}".formatted(venue.courtId(), GAME_DAY, time);
	}

	private static String id(ResultActions result) throws Exception {
		return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request, String json) throws Exception {
		request.header("Authorization", user.bearer());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request);
	}

}
