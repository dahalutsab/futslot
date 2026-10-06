package com.futslot.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

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
class RecurringBookingTests {

	private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Kathmandu"));

	private static final LocalDate TOMORROW = TODAY.plusDays(1);

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Autowired
	RecurringBookingService recurringBookingService;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void weeklySeriesBooksTheNextEightWeeksAndTellsThePlayerOnce() throws Exception {
		TestUser owner = this.users.signUp("Weekly Owner");
		TestVenue venue = this.venues.active(owner, "Weekly Futsal");
		TestUser player = this.users.signUp("Team Captain");

		String body = create(owner, venue, series(venue, TOMORROW, null, phoneOf(player))).andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.price").value(1800))
			.andExpect(jsonPath("$.upcoming.length()").value(8))
			.andExpect(jsonPath("$.upcoming[*].source", everyItem(is("RECURRING"))))
			.andExpect(jsonPath("$.upcoming[0].startsAt").value(TOMORROW + "T19:00:00+05:45"))
			.andExpect(jsonPath("$.upcoming[1].startsAt").value(TOMORROW.plusWeeks(1) + "T19:00:00+05:45"))
			.andExpect(jsonPath("$.skipped.length()").value(0))
			.andReturn().getResponse().getContentAsString();

		this.mvc.perform(get("/api/v1/venues/" + venue.slug() + "/availability").param("date", TOMORROW.plusWeeks(1).toString()))
			.andExpect(jsonPath("$.courts[0].slots[13].status").value("BOOKED"));
		List<String> types = JsonPath.read(send(player, get("/api/v1/notifications"), null).andReturn().getResponse()
			.getContentAsString(), "$.content[*].type");
		assertThat(types).containsExactly("RECURRING_BOOKING_CREATED");
		send(player, get("/api/v1/bookings"), null).andExpect(jsonPath("$.length()").value(8));
		send(owner, get(base(venue)), null).andExpect(jsonPath("$[*].id", hasItem((String) JsonPath.read(body, "$.id"))));
	}

	@Test
	void takenAndBlockedWeeksAreSkippedNotFailed() throws Exception {
		TestUser owner = this.users.signUp("Skip Owner");
		TestVenue venue = this.venues.active(owner, "Skip Futsal");
		send(owner, post("/api/v1/owner/venues/" + venue.id() + "/bookings"), """
				{"courtId":"%s","startsAt":"%sT19:00:00+05:45","customerName":"Other","customerPhone":"9811999001","source":"PHONE"}"""
			.formatted(venue.courtId(), TOMORROW.plusWeeks(2))).andExpect(status().isCreated());
		send(owner, post("/api/v1/owner/venues/" + venue.id() + "/courts/" + venue.courtId() + "/blocks"), """
				{"startsAt":"%sT18:00:00+05:45","endsAt":"%sT22:00:00+05:45","reason":"PRIVATE_EVENT"}"""
			.formatted(TOMORROW.plusWeeks(3), TOMORROW.plusWeeks(3))).andExpect(status().isCreated());

		create(owner, venue, series(venue, TOMORROW, null, "9811999002")).andExpect(status().isCreated())
			.andExpect(jsonPath("$.upcoming.length()").value(6))
			.andExpect(jsonPath("$.skipped[0].date").value(TOMORROW.plusWeeks(2).toString()))
			.andExpect(jsonPath("$.skipped[0].reason").value("SLOT_UNAVAILABLE"))
			.andExpect(jsonPath("$.skipped[1].date").value(TOMORROW.plusWeeks(3).toString()))
			.andExpect(jsonPath("$.skipped[1].reason").value("SLOT_BLOCKED"));
	}

	@Test
	void seriesStopsAtItsEndDate() throws Exception {
		TestUser owner = this.users.signUp("End Owner");
		TestVenue venue = this.venues.active(owner, "End Futsal");

		create(owner, venue, series(venue, TOMORROW, TOMORROW.plusWeeks(2), "9811999003")).andExpect(status().isCreated())
			.andExpect(jsonPath("$.upcoming.length()").value(3))
			.andExpect(jsonPath("$.generatedUntil").value(TOMORROW.plusWeeks(2).toString()));
	}

	@Test
	void pausingFreesFutureWeeksAndResumingBooksThemAgain() throws Exception {
		TestUser owner = this.users.signUp("Pause Owner");
		TestVenue venue = this.venues.active(owner, "Pause Futsal");
		String id = JsonPath.read(create(owner, venue, series(venue, TOMORROW, null, "9811999004")).andReturn().getResponse()
			.getContentAsString(), "$.id");

		send(owner, post(base(venue) + "/" + id + "/pause"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PAUSED"))
			.andExpect(jsonPath("$.upcoming.length()").value(0));
		this.mvc.perform(get("/api/v1/venues/" + venue.slug() + "/availability").param("date", TOMORROW.toString()))
			.andExpect(jsonPath("$.courts[0].slots[13].status").value("AVAILABLE"));
		send(owner, post(base(venue) + "/" + id + "/pause"), null).andExpect(status().isConflict());

		send(owner, post(base(venue) + "/" + id + "/resume"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.upcoming.length()").value(8));
	}

	@Test
	void cancellingEndsTheSeriesAndTellsThePlayer() throws Exception {
		TestUser owner = this.users.signUp("End Series Owner");
		TestVenue venue = this.venues.active(owner, "End Series Futsal");
		TestUser player = this.users.signUp("Former Regular");
		String id = JsonPath.read(create(owner, venue, series(venue, TOMORROW, null, phoneOf(player))).andReturn()
			.getResponse().getContentAsString(), "$.id");

		send(owner, post(base(venue) + "/" + id + "/cancel"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"))
			.andExpect(jsonPath("$.upcoming.length()").value(0));
		send(owner, post(base(venue) + "/" + id + "/cancel"), null).andExpect(status().isConflict());
		send(owner, get(base(venue)), null).andExpect(jsonPath("$[*].id", not(hasItem(id))));
		send(player, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("RECURRING_BOOKING_CANCELLED"));
		send(player, get("/api/v1/bookings"), null).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void topUpBooksNewWeeksAndReportsClashesToTheVenue() throws Exception {
		TestUser owner = this.users.signUp("Topup Owner");
		TestVenue venue = this.venues.active(owner, "Topup Futsal");
		String id = JsonPath.read(create(owner, venue, series(venue, TOMORROW, null, "9811999005")).andReturn()
			.getResponse().getContentAsString(), "$.id");
		// Pretend only the first week had been generated, and someone took week 2 in the meantime
		this.jdbc.update("DELETE FROM bookings WHERE recurring_booking_id = ?::uuid AND starts_at > ?::timestamptz", id,
				TOMORROW + "T23:59:00+05:45");
		this.jdbc.update("UPDATE recurring_bookings SET generated_until = ? WHERE id = ?::uuid", TOMORROW, id);
		send(owner, post("/api/v1/owner/venues/" + venue.id() + "/bookings"), """
				{"courtId":"%s","startsAt":"%sT19:00:00+05:45","customerName":"Walk In","customerPhone":"9811999006","source":"WALK_IN"}"""
			.formatted(venue.courtId(), TOMORROW.plusWeeks(1))).andExpect(status().isCreated());

		assertThat(this.recurringBookingService.needingGeneration()).contains(UUID.fromString(id));
		this.recurringBookingService.extend(UUID.fromString(id));

		send(owner, get(base(venue) + "/" + id), null).andExpect(jsonPath("$.upcoming.length()").value(7));
		send(owner, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("RECURRING_BOOKING_SKIPPED"));
		assertThat(this.recurringBookingService.needingGeneration()).doesNotContain(UUID.fromString(id));
	}

	@Test
	void invalidSeriesAreRejected() throws Exception {
		TestUser owner = this.users.signUp("Invalid Owner");
		TestVenue venue = this.venues.active(owner, "Invalid Series Futsal");
		String day = TOMORROW.getDayOfWeek().name();

		create(owner, venue, """
				{"courtId":"%s","dayOfWeek":"%s","startTime":"20:00","endTime":"19:00","startsOn":"%s",
				 "customerName":"X","customerPhone":"9811999007"}""".formatted(venue.courtId(), day, TOMORROW))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));
		create(owner, venue, """
				{"courtId":"%s","dayOfWeek":"%s","startTime":"19:00","startsOn":"%s","customerName":"X","customerPhone":"9811999007"}"""
			.formatted(venue.courtId(), day, TODAY.minusDays(7)))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
		create(owner, venue, """
				{"courtId":"%s","dayOfWeek":"%s","startTime":"19:30","startsOn":"%s","customerName":"X","customerPhone":"9811999007"}"""
			.formatted(venue.courtId(), day, TOMORROW))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PRICE_REQUIRED"));
	}

	private static String series(TestVenue venue, LocalDate startsOn, LocalDate endsOn, String phone) {
		return """
				{"courtId":"%s","dayOfWeek":"%s","startTime":"19:00","startsOn":"%s",%s"customerName":"Regulars FC",
				 "customerPhone":"%s"}""".formatted(venue.courtId(), startsOn.getDayOfWeek(), startsOn,
				(endsOn != null) ? "\"endsOn\":\"" + endsOn + "\"," : "", phone);
	}

	private ResultActions create(TestUser owner, TestVenue venue, String json) throws Exception {
		return send(owner, post(base(venue)), json);
	}

	private static String base(TestVenue venue) {
		return "/api/v1/owner/venues/" + venue.id() + "/recurring-bookings";
	}

	private String phoneOf(TestUser user) throws Exception {
		return JsonPath.read(send(user, get("/api/v1/me"), null).andReturn().getResponse().getContentAsString(), "$.phone");
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request, String json) throws Exception {
		request.header("Authorization", user.bearer());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request);
	}

}
