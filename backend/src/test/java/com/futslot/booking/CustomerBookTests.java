package com.futslot.booking;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class CustomerBookTests {

	private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Kathmandu"));

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Test
	void customersAreListedSearchedAndSortedFromBookings() throws Exception {
		TestUser owner = this.users.signUp("Book Keeper");
		TestVenue venue = this.venues.active(owner, "Customer Futsal");
		book(owner, venue, TODAY.plusDays(1), "06:00", "Anita Gurung", "9811000001");
		book(owner, venue, TODAY.plusDays(1), "07:00", "Anita Gurung", "9811000001");
		book(owner, venue, TODAY.plusDays(2), "06:00", "Bijay Shah", "9822000002");
		String customers = "/api/v1/owner/venues/" + venue.id() + "/customers";

		send(owner, get(customers), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.content[*].name", contains("Bijay Shah", "Anita Gurung")))
			.andExpect(jsonPath("$.content[0].hasApp").value(false));
		send(owner, get(customers).param("sort", "FREQUENT"), null)
			.andExpect(jsonPath("$.content[0].name").value("Anita Gurung"))
			.andExpect(jsonPath("$.content[0].totalBookings").value(2));
		send(owner, get(customers).param("q", "anita"), null).andExpect(jsonPath("$.totalElements").value(1));
		send(owner, get(customers).param("q", "982-200"), null)
			.andExpect(jsonPath("$.content[*].name", contains("Bijay Shah")));
		send(owner, get(customers).param("q", "100%"), null).andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void customerDetailShowsHistoryStatsAndEditableNotes() throws Exception {
		TestUser owner = this.users.signUp("Detail Keeper");
		TestVenue venue = this.venues.active(owner, "Detail Futsal");
		String bookings = "/api/v1/owner/venues/" + venue.id() + "/bookings";
		String past = id(book(owner, venue, TODAY.minusDays(2), "06:00", "Chandra", "9833000003"));
		String noShow = id(book(owner, venue, TODAY.minusDays(1), "06:00", "Chandra", "9833000003"));
		String cancelled = id(book(owner, venue, TODAY.plusDays(1), "06:00", "Chandra", "9833000003"));
		book(owner, venue, TODAY.plusDays(2), "06:00", "Chandra", "9833000003");
		send(owner, post(bookings + "/" + past + "/complete"), null).andExpect(status().isOk());
		send(owner, post(bookings + "/" + noShow + "/no-show"), null).andExpect(status().isOk());
		send(owner, post(bookings + "/" + cancelled + "/cancel"), null).andExpect(status().isOk());
		String customerId = JsonPath.read(send(owner, get("/api/v1/owner/venues/" + venue.id() + "/customers"), null)
			.andReturn().getResponse().getContentAsString(), "$.content[0].id");
		String customer = "/api/v1/owner/venues/" + venue.id() + "/customers/" + customerId;

		send(owner, get(customer), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.stats.total").value(4))
			.andExpect(jsonPath("$.stats.completed").value(1))
			.andExpect(jsonPath("$.stats.noShows").value(1))
			.andExpect(jsonPath("$.stats.cancelled").value(1))
			.andExpect(jsonPath("$.stats.upcoming").value(1))
			.andExpect(jsonPath("$.stats.completedValue").value(1200))
			.andExpect(jsonPath("$.recentBookings.length()").value(4))
			.andExpect(jsonPath("$.recentBookings[0].startsAt").value(TODAY.plusDays(2) + "T06:00:00+05:45"));

		send(owner, patch(customer), "{\"name\":\"Chandra Bahadur\",\"notes\":\"Prefers Court A\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Chandra Bahadur"))
			.andExpect(jsonPath("$.notes").value("Prefers Court A"));
		send(owner, patch(customer), "{\"notes\":\"\"}").andExpect(jsonPath("$.notes").doesNotExist());
	}

	@Test
	void customerBookIsPrivateToTheVenue() throws Exception {
		TestUser owner = this.users.signUp("Private Keeper");
		TestVenue venue = this.venues.active(owner, "Private Customers Futsal");
		book(owner, venue, TODAY.plusDays(1), "06:00", "Secret", "9844000004");
		String customerId = JsonPath.read(send(owner, get("/api/v1/owner/venues/" + venue.id() + "/customers"), null)
			.andReturn().getResponse().getContentAsString(), "$.content[0].id");
		TestUser otherOwner = this.users.signUp("Other Keeper");
		TestVenue otherVenue = this.venues.active(otherOwner, "Other Customers Futsal");

		send(otherOwner, get("/api/v1/owner/venues/" + venue.id() + "/customers"), null).andExpect(status().isNotFound());
		send(otherOwner, get("/api/v1/owner/venues/" + otherVenue.id() + "/customers/" + customerId), null)
			.andExpect(status().isNotFound());

		String phone = TestUsers.nextLocalPhone();
		send(owner, post("/api/v1/owner/venues/" + venue.id() + "/staff"),
				"{\"phone\":\"%s\",\"fullName\":\"Limited\",\"role\":\"RECEPTIONIST\",\"permissions\":[\"MANAGE_BOOKINGS\"]}"
					.formatted(phone)).andExpect(status().isCreated());
		send(this.users.logIn(phone, "Limited"), get("/api/v1/owner/venues/" + venue.id() + "/customers"), null)
			.andExpect(status().isForbidden());
	}

	private ResultActions book(TestUser owner, TestVenue venue, LocalDate date, String time, String name, String phone)
			throws Exception {
		return send(owner, post("/api/v1/owner/venues/" + venue.id() + "/bookings"), """
				{"courtId":"%s","startsAt":"%sT%s:00+05:45","customerName":"%s","customerPhone":"%s","source":"PHONE"}"""
			.formatted(venue.courtId(), date, time, name, phone)).andExpect(status().isCreated());
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
