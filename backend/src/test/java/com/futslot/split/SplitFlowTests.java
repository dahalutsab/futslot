package com.futslot.split;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
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
class SplitFlowTests {

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
	void baaziLosersPaySeventyPercentAndTheBookerCollects() throws Exception {
		TestVenue venue = this.venues.active(this.users.signUp("Baazi Owner"), "Baazi Futsal");
		TestUser ram = this.users.signUp("Ram Karki");
		String bookingId = book(ram, venue, "07:00");
		String split = "/api/v1/bookings/" + bookingId + "/split";

		send(ram, put(split), "{\"rule\":\"LOSERS_PAY_PERCENT\"}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("PERCENT_REQUIRED"));
		send(ram, put(split), """
				{"rule":"LOSERS_PAY_PERCENT","losersPercent":70,"sideAName":"Bibs","sideBName":"No bibs"}""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.players.length()").value(1))
			.andExpect(jsonPath("$.players[0].booker").value(true))
			.andExpect(jsonPath("$.players[0].side").value("A"));

		String sitaPhone = TestUsers.nextLocalPhone();
		TestUser sita = this.users.logIn(sitaPhone, "Sita Shah");
		send(ram, post(split + "/players"), "{\"phone\":\"%s\",\"side\":\"A\"}".formatted(sitaPhone))
			.andExpect(status().isOk());
		send(ram, post(split + "/players"), "{\"phone\":\"%s\",\"side\":\"B\"}".formatted(sitaPhone))
			.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_ADDED"));
		send(ram, post(split + "/players"), "{\"name\":\"Hari\",\"side\":\"B\"}").andExpect(status().isOk());
		String body = send(ram, post(split + "/players"), "{\"name\":\"Gopal\",\"phone\":\"9800001111\",\"side\":\"B\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.players[3].phone").value("+9779800001111"))
			.andReturn().getResponse().getContentAsString();
		String hari = JsonPath.read(body, "$.players[2].id");
		send(ram, post(split + "/players"), "{\"phone\":\"%s\",\"side\":\"B\"}".formatted(TestUsers.nextLocalPhone()))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NAME_REQUIRED"));
		send(sita, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("SPLIT_ADDED"))
			.andExpect(jsonPath("$.content[0].body").value(containsString("You're on Bibs")));

		// Everyone sees the stakes before kick-off; only the booker sees the players' numbers
		send(sita, get(split), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.players[3].phone").doesNotExist())
			.andExpect(jsonPath("$.bookedBy.name").value("Ram Karki"))
			.andExpect(jsonPath("$.outcomes[0].result").value("A_WON"))
			.andExpect(jsonPath("$.outcomes[0].sideAEach").value(180))
			.andExpect(jsonPath("$.outcomes[0].sideBEach").value(420))
			.andExpect(jsonPath("$.outcomes[2].sideAEach").value(300));
		send(this.users.signUp("Nosy"), get(split), null).andExpect(status().isNotFound());

		send(ram, post(split + "/result"), "{\"result\":\"B_WON\"}").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MATCH_NOT_STARTED"));
		played(bookingId);
		send(ram, post(split + "/result"), "{\"result\":\"B_WON\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.result").value("B_WON"))
			.andExpect(jsonPath("$.players[0].share").value(420.0))
			.andExpect(jsonPath("$.players[0].settled").value(true))
			.andExpect(jsonPath("$.players[1].share").value(420.0))
			.andExpect(jsonPath("$.players[2].share").value(180.0))
			.andExpect(jsonPath("$.collected").value(420.0))
			.andExpect(jsonPath("$.outstanding").value(780.0))
			.andExpect(jsonPath("$.outcomes").doesNotExist());
		send(sita, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("SPLIT_RESULT"))
			.andExpect(jsonPath("$.content[0].title").value("No bibs won"))
			.andExpect(jsonPath("$.content[0].body").value(containsString("Your share is Rs 420, pay Ram Karki")));
		send(sita, get("/api/v1/me/split-dues"), null)
			.andExpect(jsonPath("$[0].amount").value(420.0))
			.andExpect(jsonPath("$[0].payTo.name").value("Ram Karki"));
		send(ram, put(split), "{\"rule\":\"EQUAL\"}").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("RESULT_RECORDED"));

		send(ram, post(split + "/players/" + hari + "/paid"), null).andExpect(jsonPath("$.collected").value(600.0));
		send(ram, delete(split + "/result"), null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_SETTLING"));
		send(ram, delete(split + "/players/" + hari + "/paid"), null).andExpect(status().isOk());
		send(ram, delete(split + "/result"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.result").doesNotExist())
			.andExpect(jsonPath("$.players[1].share").doesNotExist())
			.andExpect(jsonPath("$.outcomes.length()").value(3));
		send(sita, get("/api/v1/me/split-dues"), null).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void winnersPayForWaterAndEqualSplitsNeedNoResult() throws Exception {
		TestVenue venue = this.venues.active(this.users.signUp("Water Owner"), "Water Futsal");
		TestUser booker = this.users.signUp("Water Booker");
		String bookingId = book(booker, venue, "08:00");
		String split = "/api/v1/bookings/" + bookingId + "/split";
		send(booker, put(split), "{\"rule\":\"LOSERS_PAY_COURT\",\"extrasAmount\":300,\"extrasNote\":\"Water\"}")
			.andExpect(jsonPath("$.total").value(1500.0));
		send(booker, post(split + "/players"), "{\"name\":\"Same Side\",\"side\":\"A\"}");
		played(bookingId);
		send(booker, post(split + "/result"), "{\"result\":\"A_WON\"}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("SIDES_INCOMPLETE"));

		send(booker, post(split + "/players"), "{\"name\":\"Other One\",\"side\":\"B\"}");
		send(booker, post(split + "/players"), "{\"name\":\"Other Two\",\"side\":\"B\"}");
		send(booker, post(split + "/result"), "{\"result\":\"A_WON\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.players[0].share").value(150.0))
			.andExpect(jsonPath("$.players[2].share").value(600.0));
		send(booker, delete(split + "/result"), null).andExpect(status().isOk());

		send(booker, put(split), "{\"rule\":\"EQUAL\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.total").value(1200.0));
		send(booker, post(split + "/result"), "{}").andExpect(status().isOk())
			.andExpect(jsonPath("$.result").value("DRAW"))
			.andExpect(jsonPath("$.players[*].share", everyItem(is(300.0))));
	}

	private String book(TestUser player, TestVenue venue, String time) throws Exception {
		return JsonPath.read(send(player, post("/api/v1/bookings"),
				"{\"courtId\":\"%s\",\"startsAt\":\"%sT%s:00+05:45\"}".formatted(venue.courtId(), GAME_DAY, time))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString(), "$.id");
	}

	/** Moves the booking to yesterday so the result can be recorded. */
	private void played(String bookingId) {
		this.jdbc.update("""
				UPDATE bookings SET starts_at = starts_at - interval '4 days', ends_at = ends_at - interval '4 days'
				WHERE id = ?::uuid""", bookingId);
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request, String json) throws Exception {
		request.header("Authorization", user.bearer());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request);
	}

}
