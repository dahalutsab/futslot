package com.futslot.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
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
class ReportFlowTests {

	private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Kathmandu"));

	private static final LocalDate GAME_DAY = TODAY.plusDays(3);

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Test
	void ownerSeesBookingsMoneyAndOccupancyForAPeriod() throws Exception {
		TestUser owner = this.users.signUp("Report Owner");
		TestVenue venue = this.venues.active(owner, "Report Futsal");
		TestUser regular = this.users.signUp("Regular Ram");
		book(regular, venue, "07:00");
		book(regular, venue, "18:00");
		String cancelled = id(book(this.users.signUp("Flaky"), venue, "08:00"));
		String walkIn = id(send(owner, post(venuePath(venue) + "/bookings"), """
				{"courtId":"%s","startsAt":"%sT09:00:00+05:45","customerName":"=HYPERLINK(\\"x\\")","customerPhone":"%s","source":"WALK_IN","price":1000}"""
			.formatted(venue.courtId(), GAME_DAY, TestUsers.nextLocalPhone())).andExpect(status().isCreated()));
		send(owner, post(venuePath(venue) + "/bookings/" + cancelled + "/cancel"), "{\"reason\":\"Rain\"}")
			.andExpect(status().isOk());
		send(owner, post(venuePath(venue) + "/bookings/" + walkIn + "/payments"), "{\"method\":\"CASH\",\"amount\":1000}")
			.andExpect(status().isCreated());
		String period = "?from=" + TODAY + "&to=" + GAME_DAY;

		send(owner, get(reports(venue) + "/summary" + period), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.bookings.total").value(4))
			.andExpect(jsonPath("$.bookings.played").value(3))
			.andExpect(jsonPath("$.bookings.cancelled").value(1))
			.andExpect(jsonPath("$.bookings.bySource.APP").value(2))
			.andExpect(jsonPath("$.bookings.bySource.WALK_IN").value(1))
			.andExpect(jsonPath("$.bookings.cancellationRate").value(25.0))
			.andExpect(jsonPath("$.money.bookedValue").value(4000.0))
			.andExpect(jsonPath("$.money.outstanding").value(3000.0))
			.andExpect(jsonPath("$.money.collected").value(1000.0))
			.andExpect(jsonPath("$.money.collectedByMethod.CASH").value(1000.0))
			.andExpect(jsonPath("$.money.net").value(1000.0))
			// Open 06:00-22:00 = 16h a day, over 4 days, on one court
			.andExpect(jsonPath("$.occupancy.openHours").value(64.0))
			.andExpect(jsonPath("$.occupancy.bookedHours").value(3.0))
			.andExpect(jsonPath("$.occupancy.rate").value(4.7))
			.andExpect(jsonPath("$.courts[0].bookings").value(3))
			.andExpect(jsonPath("$.courts[0].bookedValue").value(4000.0));

		send(owner, get(reports(venue) + "/daily" + period), null)
			.andExpect(jsonPath("$.length()").value(4))
			.andExpect(jsonPath("$[0].date").value(TODAY.toString()))
			.andExpect(jsonPath("$[0].collected").value(1000.0))
			.andExpect(jsonPath("$[3].bookings").value(3))
			.andExpect(jsonPath("$[3].cancelled").value(1))
			.andExpect(jsonPath("$[3].bookedHours").value(3.0));
		send(owner, get(reports(venue) + "/peak-hours" + period), null)
			.andExpect(jsonPath("$[*].hour", containsInAnyOrder(7, 9, 18)))
			.andExpect(jsonPath("$[0].dayOfWeek").value(GAME_DAY.getDayOfWeek().name()));
		send(owner, get(reports(venue) + "/top-customers" + period), null)
			.andExpect(jsonPath("$[0].name").value("Regular Ram"))
			.andExpect(jsonPath("$[0].bookings").value(2))
			.andExpect(jsonPath("$[0].spent").value(3000.0));

		String csv = new String(send(owner, get(reports(venue) + "/bookings.csv" + period), null)
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Disposition",
					"attachment; filename=\"" + venue.slug() + "-bookings-" + TODAY + "-to-" + GAME_DAY + ".csv\""))
			.andReturn().getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
		String[] lines = csv.split("\r\n");
		assertThat(lines).hasSize(5);
		assertThat(lines[0]).startsWith("﻿Reference,Date,Start");
		assertThat(csv).contains(GAME_DAY + ",07:00,08:00,Court A,Regular Ram,+977")
			.contains("\"'=HYPERLINK(\"\"x\"\")\"")
			.contains("WALK_IN,CONFIRMED,1000.00,0.00,1000.00,1000.00,0.00,PAID");
	}

	@Test
	void reportsNeedReportAccessAndASensiblePeriod() throws Exception {
		TestUser owner = this.users.signUp("Private Report Owner");
		TestVenue venue = this.venues.active(owner, "Private Report Futsal");
		String receptionistPhone = TestUsers.nextLocalPhone();
		send(owner, post(venuePath(venue) + "/staff"),
				"{\"phone\":\"%s\",\"fullName\":\"Desk\",\"role\":\"RECEPTIONIST\"}".formatted(receptionistPhone))
			.andExpect(status().isCreated());
		String managerPhone = TestUsers.nextLocalPhone();
		send(owner, post(venuePath(venue) + "/staff"),
				"{\"phone\":\"%s\",\"fullName\":\"Boss\",\"role\":\"MANAGER\"}".formatted(managerPhone))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.permissions", hasItem("VIEW_REPORTS")));

		send(this.users.logIn(receptionistPhone, "Desk"), get(reports(venue) + "/summary"), null)
			.andExpect(status().isForbidden());
		send(this.users.logIn(managerPhone, "Boss"), get(reports(venue) + "/summary"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.to").value(TODAY.toString()))
			.andExpect(jsonPath("$.from").value(TODAY.minusDays(29).toString()))
			.andExpect(jsonPath("$.bookings.total").value(0))
			.andExpect(jsonPath("$.bookings.cancellationRate").doesNotExist());
		send(this.users.signUp("Snoop"), get(reports(venue) + "/summary"), null).andExpect(status().isNotFound());
		send(owner, get(reports(venue) + "/summary?from=" + TODAY + "&to=" + TODAY.minusDays(1)), null)
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PERIOD"));
		send(owner, get(reports(venue) + "/daily?from=" + TODAY.minusDays(400) + "&to=" + TODAY), null)
			.andExpect(status().isBadRequest());
	}

	@Test
	void adminsSeePlatformTotals() throws Exception {
		TestUser owner = this.users.signUp("Platform Owner");
		TestVenue venue = this.venues.active(owner, "Platform Futsal");
		book(this.users.signUp("Platform Player"), venue, "10:00");
		String period = "?from=" + TODAY + "&to=" + GAME_DAY;

		send(owner, get("/api/v1/admin/reports/summary" + period), null).andExpect(status().isForbidden());
		String body = send(this.users.signUpAdmin("Platform Admin"), get("/api/v1/admin/reports/summary" + period), null)
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		assertThat(((Number) JsonPath.read(body, "$.newUsers")).longValue()).isGreaterThanOrEqualTo(3);
		assertThat(((Number) JsonPath.read(body, "$.playedBookings")).longValue()).isGreaterThanOrEqualTo(1);
		assertThat(((Number) JsonPath.read(body, "$.venuesByStatus.ACTIVE")).longValue()).isGreaterThanOrEqualTo(1);
	}

	private ResultActions book(TestUser player, TestVenue venue, String time) throws Exception {
		return send(player, post("/api/v1/bookings"),
				"{\"courtId\":\"%s\",\"startsAt\":\"%sT%s:00+05:45\"}".formatted(venue.courtId(), GAME_DAY, time))
			.andExpect(status().isCreated());
	}

	private static String venuePath(TestVenue venue) {
		return "/api/v1/owner/venues/" + venue.id();
	}

	private static String reports(TestVenue venue) {
		return venuePath(venue) + "/reports";
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
