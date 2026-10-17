package com.futslot.venue;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
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
class StaffAccessTests {

	private static final LocalDate TOMORROW = LocalDate.now(ZoneId.of("Asia/Kathmandu")).plusDays(1);

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Test
	void receptionistAddedByPhoneCanRunBookingsButNotTheVenue() throws Exception {
		TestUser owner = this.users.signUp("Staff Owner");
		TestVenue venue = this.venues.active(owner, "Staff Futsal");
		String phone = TestUsers.nextLocalPhone();

		send(owner, post(staff(venue)), staffJson(phone, "Rita Desk", "RECEPTIONIST", null)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.fullName").value("Rita Desk"))
			.andExpect(jsonPath("$.permissions", containsInAnyOrder("MANAGE_BOOKINGS", "MANAGE_BLOCKS", "VIEW_CUSTOMERS")));
		TestUser receptionist = this.users.logIn(phone, "ignored");

		send(receptionist, get("/api/v1/owner/venues"), null).andExpect(jsonPath("$[0].venue.id").value(venue.id().toString()))
			.andExpect(jsonPath("$[0].access").value("RECEPTIONIST"));
		send(receptionist, post("/api/v1/owner/venues/" + venue.id() + "/bookings"), """
				{"courtId":"%s","startsAt":"%sT06:00:00+05:45","customerName":"Walk In","customerPhone":"9810000999","source":"WALK_IN"}"""
			.formatted(venue.courtId(), TOMORROW)).andExpect(status().isCreated());
		send(receptionist, get("/api/v1/owner/venues/" + venue.id() + "/availability").param("date", TOMORROW.toString()), null)
			.andExpect(status().isOk());

		send(receptionist, put("/api/v1/owner/venues/" + venue.id() + "/policy"), """
				{"advancePercent":0,"bookingHoldMinutes":15,"cancellationTiers":[{"withinHours":24,"feePercent":100}],"maxAdvanceBookingDays":30}""")
			.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
		send(receptionist, get(staff(venue)), null).andExpect(status().isForbidden());
		send(receptionist, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[*].type", hasItem("VENUE_STAFF_ADDED")));
	}

	@Test
	void managerCanChangeTheVenueButNeverManageStaff() throws Exception {
		TestUser owner = this.users.signUp("Mgr Owner");
		TestVenue venue = this.venues.active(owner, "Mgr Futsal");
		String phone = TestUsers.nextLocalPhone();
		send(owner, post(staff(venue)), staffJson(phone, "Manny", "MANAGER", null)).andExpect(status().isCreated());
		TestUser manager = this.users.logIn(phone, "Manny");

		send(manager, put("/api/v1/owner/venues/" + venue.id() + "/policy"), """
				{"advancePercent":10,"bookingHoldMinutes":15,"cancellationTiers":[{"withinHours":24,"feePercent":100}],"maxAdvanceBookingDays":30}""")
			.andExpect(status().isOk());
		send(manager, post(staff(venue)), staffJson(TestUsers.nextLocalPhone(), "Sneaky", "RECEPTIONIST", null))
			.andExpect(status().isForbidden());
		send(owner, post(staff(venue)), staffJson(TestUsers.nextLocalPhone(), "Boss", "MANAGER", "[\"MANAGE_STAFF\"]"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PERMISSION_NOT_GRANTABLE"));
	}

	@Test
	void permissionsCanBeNarrowedAndAccessRemoved() throws Exception {
		TestUser owner = this.users.signUp("Narrow Owner");
		TestVenue venue = this.venues.active(owner, "Narrow Futsal");
		String phone = TestUsers.nextLocalPhone();
		String staffId = JsonPath.read(send(owner, post(staff(venue)),
				staffJson(phone, "Viewer", "RECEPTIONIST", "[\"VIEW_CUSTOMERS\"]")).andReturn().getResponse()
			.getContentAsString(), "$.id");
		TestUser viewer = this.users.logIn(phone, "Viewer");
		String manual = """
				{"courtId":"%s","startsAt":"%sT07:00:00+05:45","customerName":"X","customerPhone":"9810000998","source":"PHONE"}"""
			.formatted(venue.courtId(), TOMORROW);

		send(viewer, post("/api/v1/owner/venues/" + venue.id() + "/bookings"), manual).andExpect(status().isForbidden());

		send(owner, put(staff(venue) + "/" + staffId), "{\"role\":\"RECEPTIONIST\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.permissions", hasItem("MANAGE_BOOKINGS")));
		send(viewer, post("/api/v1/owner/venues/" + venue.id() + "/bookings"), manual).andExpect(status().isCreated());

		send(owner, delete(staff(venue) + "/" + staffId), null).andExpect(status().isNoContent());
		send(viewer, get("/api/v1/owner/venues/" + venue.id()), null).andExpect(status().isNotFound());
		send(viewer, get("/api/v1/owner/venues"), null).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void bookingStaffAreNotifiedOfNewAppBookings() throws Exception {
		TestUser owner = this.users.signUp("Notify Staff Owner");
		TestVenue venue = this.venues.active(owner, "Notify Staff Futsal");
		String phone = TestUsers.nextLocalPhone();
		send(owner, post(staff(venue)), staffJson(phone, "Desk", "RECEPTIONIST", null)).andExpect(status().isCreated());
		TestUser receptionist = this.users.logIn(phone, "Desk");
		TestUser player = this.users.signUp("Booking Player");

		send(player, post("/api/v1/bookings"),
				"{\"courtId\":\"%s\",\"startsAt\":\"%sT08:00:00+05:45\"}".formatted(venue.courtId(), TOMORROW))
			.andExpect(status().isCreated());

		send(receptionist, get("/api/v1/notifications"), null).andExpect(jsonPath("$.content[0].type").value("NEW_BOOKING"));
		send(owner, get("/api/v1/notifications"), null).andExpect(jsonPath("$.content[0].type").value("NEW_BOOKING"));
	}

	@Test
	void ownerCannotBeAddedAsStaffAndStrangersSeeNothing() throws Exception {
		TestUser owner = this.users.signUp("Self Owner");
		TestVenue venue = this.venues.active(owner, "Self Futsal");
		String ownerPhone = JsonPath.read(send(owner, get("/api/v1/me"), null).andReturn().getResponse().getContentAsString(),
				"$.phone");

		send(owner, post(staff(venue)), staffJson(ownerPhone, "Me", "MANAGER", null)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("ALREADY_OWNER"));
		send(this.users.signUp("Stranger"), get(staff(venue)), null).andExpect(status().isNotFound());
	}

	private static String staff(TestVenue venue) {
		return "/api/v1/owner/venues/" + venue.id() + "/staff";
	}

	private static String staffJson(String phone, String name, String role, String permissions) {
		return "{\"phone\":\"%s\",\"fullName\":\"%s\",\"role\":\"%s\"%s}".formatted(phone, name, role,
				(permissions != null) ? ",\"permissions\":" + permissions : "");
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request, String json) throws Exception {
		request.header("Authorization", user.bearer());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request);
	}

}
