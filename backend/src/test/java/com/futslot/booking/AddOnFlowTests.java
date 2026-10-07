package com.futslot.booking;

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
class AddOnFlowTests {

	private static final LocalDate GAME_DAY = LocalDate.now(ZoneId.of("Asia/Kathmandu")).plusDays(3);

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Test
	void extrasAddToTheBookingAndStaffCanAddMoreAtTheCounter() throws Exception {
		TestUser owner = this.users.signUp("Extras Owner");
		TestVenue venue = this.venues.active(owner, "Extras Futsal");
		send(owner, put(venuePath(venue) + "/policy"), """
				{"advancePercent":30,"bookingHoldMinutes":15,"freeCancelHours":24,"lateCancelRefundPct":0,"maxAdvanceBookingDays":30}""");
		String water = item(owner, venue, "Water (1L)", "FOOD_DRINK", 50);
		String bibs = item(owner, venue, "Bibs", "EQUIPMENT_RENTAL", 100);
		send(owner, post(venuePath(venue) + "/add-ons"), "{\"name\":\"water (1l)\",\"category\":\"FOOD_DRINK\",\"price\":60}")
			.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ADD_ON_EXISTS"));
		this.mvc.perform(get("/api/v1/venues/" + venue.slug() + "/add-ons"))
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].name").value("Bibs"));

		TestUser player = this.users.signUp("Thirsty Player");
		String withWater = "[{\"itemId\":\"%s\",\"quantity\":2}]".formatted(water);
		send(player, post("/api/v1/bookings/quote"), booking(venue, "07:00", withWater)).andExpect(status().isOk())
			.andExpect(jsonPath("$.price").value(1200.0))
			.andExpect(jsonPath("$.addOns").value(100.0))
			.andExpect(jsonPath("$.total").value(1300.0))
			.andExpect(jsonPath("$.advanceRequired").value(390.0));
		String bookingId = JsonPath.read(send(player, post("/api/v1/bookings"), booking(venue, "07:00", withWater))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.addOnsAmount").value(100.0))
			.andExpect(jsonPath("$.addOns[0].name").value("Water (1L)"))
			.andExpect(jsonPath("$.addOns[0].quantity").value(2))
			.andExpect(jsonPath("$.totalAmount").value(1300.0))
			.andExpect(jsonPath("$.advanceRequired").value(390.0))
			.andReturn().getResponse().getContentAsString(), "$.id");

		TestVenue elsewhere = this.venues.active(this.users.signUp("Elsewhere Owner"), "Elsewhere Futsal");
		send(player, post("/api/v1/bookings"), """
				{"courtId":"%s","startsAt":"%sT07:00:00+05:45","addOns":[{"itemId":"%s","quantity":1}]}"""
			.formatted(elsewhere.courtId(), GAME_DAY, water)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("ADD_ON_NOT_AVAILABLE"));

		// Paid in full at the counter, then they take bibs too
		send(owner, post(venuePath(venue) + "/bookings/" + bookingId + "/payments"), "{\"method\":\"CASH\",\"amount\":1300}")
			.andExpect(status().isCreated());
		String both = "{\"addOns\":[{\"itemId\":\"%s\",\"quantity\":2},{\"itemId\":\"%s\",\"quantity\":1}]}".formatted(water, bibs);
		send(owner, put(venuePath(venue) + "/bookings/" + bookingId + "/add-ons"), both).andExpect(status().isOk())
			.andExpect(jsonPath("$.totalAmount").value(1400.0))
			.andExpect(jsonPath("$.paymentStatus").value("PARTIALLY_PAID"));
		send(owner, get(venuePath(venue) + "/bookings/" + bookingId + "/payments"), null)
			.andExpect(jsonPath("$.amountDue").value(100.0));

		// A later price change doesn't touch what this booking was charged
		send(owner, put(venuePath(venue) + "/add-ons/" + bibs), "{\"name\":\"Bibs\",\"category\":\"EQUIPMENT_RENTAL\",\"price\":150}")
			.andExpect(status().isOk());
		send(owner, put(venuePath(venue) + "/bookings/" + bookingId + "/add-ons"), both)
			.andExpect(jsonPath("$.totalAmount").value(1400.0));
		send(owner, put(venuePath(venue) + "/add-ons/" + water), "{\"name\":\"Water (1L)\",\"category\":\"FOOD_DRINK\",\"price\":50,\"active\":false}")
			.andExpect(jsonPath("$.active").value(false));
		send(player, post("/api/v1/bookings"), booking(venue, "08:00", withWater)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("ADD_ON_NOT_AVAILABLE"));
		send(owner, put(venuePath(venue) + "/bookings/" + bookingId + "/add-ons"), "{\"addOns\":[]}")
			.andExpect(jsonPath("$.totalAmount").value(1200.0))
			.andExpect(jsonPath("$.paymentStatus").value("PAID"));
	}

	private String item(TestUser owner, TestVenue venue, String name, String category, int price) throws Exception {
		return JsonPath.read(send(owner, post(venuePath(venue) + "/add-ons"),
				"{\"name\":\"%s\",\"category\":\"%s\",\"price\":%d}".formatted(name, category, price))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
	}

	private static String booking(TestVenue venue, String time, String addOns) {
		return "{\"courtId\":\"%s\",\"startsAt\":\"%sT%s:00+05:45\",\"addOns\":%s}".formatted(venue.courtId(), GAME_DAY,
				time, addOns);
	}

	private static String venuePath(TestVenue venue) {
		return "/api/v1/owner/venues/" + venue.id();
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request, String json) throws Exception {
		request.header("Authorization", user.bearer());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request);
	}

}
