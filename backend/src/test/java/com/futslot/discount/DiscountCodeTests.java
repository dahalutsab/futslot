package com.futslot.discount;

import static org.hamcrest.Matchers.hasItem;
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
class DiscountCodeTests {

	private static final LocalDate GAME_DAY = LocalDate.now(ZoneId.of("Asia/Kathmandu")).plusDays(3);

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Test
	void codesTakeMoneyOffWithinTheirLimitsAndAreGivenBackOnCancel() throws Exception {
		TestUser owner = this.users.signUp("Code Owner");
		TestVenue venue = this.venues.active(owner, "Code Futsal");
		send(owner, post(codes(venue)), """
				{"code":"welcome10","type":"PERCENT","value":10,"maxDiscountAmount":100,"usageLimit":2,"perUserLimit":1}""")
			.andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("WELCOME10"));
		send(owner, post(codes(venue)), "{\"code\":\"Welcome10\",\"type\":\"FIXED\",\"value\":50}")
			.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CODE_TAKEN"));
		send(owner, post(codes(venue)), "{\"code\":\"BIG\",\"type\":\"PERCENT\",\"value\":150}")
			.andExpect(status().isBadRequest());

		TestUser first = this.users.signUp("Code First");
		send(first, post("/api/v1/bookings/quote"), booking(venue, "07:00", "Welcome10")).andExpect(status().isOk())
			.andExpect(jsonPath("$.discount").value(100.0))
			.andExpect(jsonPath("$.total").value(1100.0))
			.andExpect(jsonPath("$.discountCode").value("WELCOME10"));
		String firstBooking = id(send(first, post("/api/v1/bookings"), booking(venue, "07:00", "welcome10"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.discountAmount").value(100.0))
			.andExpect(jsonPath("$.totalAmount").value(1100.0)));
		send(first, post("/api/v1/bookings"), booking(venue, "08:00", "WELCOME10")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("DISCOUNT_ALREADY_USED"));

		send(this.users.signUp("Code Second"), post("/api/v1/bookings"), booking(venue, "09:00", "WELCOME10"))
			.andExpect(status().isCreated());
		TestUser third = this.users.signUp("Code Third");
		send(third, post("/api/v1/bookings"), booking(venue, "10:00", "WELCOME10")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("DISCOUNT_USED_UP"));
		send(owner, get(codes(venue)), null).andExpect(jsonPath("$[0].usedCount").value(2));

		// A cancelled booking gives its use back
		send(first, post("/api/v1/bookings/" + firstBooking + "/cancel"), "{}").andExpect(status().isOk());
		send(third, post("/api/v1/bookings"), booking(venue, "10:00", "WELCOME10")).andExpect(status().isCreated());
		send(owner, get(codes(venue)), null).andExpect(jsonPath("$[0].usedCount").value(2));
	}

	@Test
	void codesOnlyWorkWhereAndWhenTheyApply() throws Exception {
		TestUser owner = this.users.signUp("Rules Code Owner");
		TestVenue venue = this.venues.active(owner, "Rules Code Futsal");
		TestVenue elsewhere = this.venues.active(this.users.signUp("Other Code Owner"), "Other Code Futsal");
		send(owner, post(codes(venue)), "{\"code\":\"OLD\",\"type\":\"FIXED\",\"value\":100,\"validTo\":\"2025-01-01T00:00:00Z\"}")
			.andExpect(status().isCreated());
		send(owner, post(codes(venue)), "{\"code\":\"BIGSPEND\",\"type\":\"FIXED\",\"value\":100,\"minBookingAmount\":1500}")
			.andExpect(status().isCreated());
		String paused = JsonPath.read(send(owner, post(codes(venue)), "{\"code\":\"PAUSED\",\"type\":\"FIXED\",\"value\":100}")
			.andReturn().getResponse().getContentAsString(), "$.id");
		send(owner, put(codes(venue) + "/" + paused), "{\"code\":\"PAUSED\",\"type\":\"FIXED\",\"value\":100,\"active\":false}")
			.andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
		send(owner, put(codes(venue) + "/" + paused), "{\"code\":\"RENAMED\",\"type\":\"FIXED\",\"value\":100}")
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CODE_IMMUTABLE"));
		send(owner, post(codes(venue)), "{\"code\":\"FREEGAME\",\"type\":\"FIXED\",\"value\":5000}")
			.andExpect(status().isCreated());
		TestUser player = this.users.signUp("Rules Code Player");

		quote(player, venue, "OLD").andExpect(jsonPath("$.code").value("DISCOUNT_NOT_VALID_NOW"));
		quote(player, venue, "BIGSPEND").andExpect(jsonPath("$.code").value("DISCOUNT_MIN_AMOUNT"));
		quote(player, venue, "PAUSED").andExpect(jsonPath("$.code").value("INVALID_DISCOUNT_CODE"));
		quote(player, elsewhere, "FREEGAME").andExpect(jsonPath("$.code").value("INVALID_DISCOUNT_CODE"));
		send(player, post("/api/v1/bookings"), booking(venue, "07:00", "FREEGAME")).andExpect(status().isCreated())
			.andExpect(jsonPath("$.totalAmount").value(0.0))
			.andExpect(jsonPath("$.paymentStatus").value("PAID"));
		send(this.users.signUp("Lister"), get(codes(venue)), null).andExpect(status().isNotFound());
		send(owner, get(codes(venue)), null).andExpect(jsonPath("$[*].code", hasItem("FREEGAME")));
	}

	private ResultActions quote(TestUser player, TestVenue venue, String code) throws Exception {
		return send(player, post("/api/v1/bookings/quote"), booking(venue, "07:00", code))
			.andExpect(status().isBadRequest());
	}

	private static String booking(TestVenue venue, String time, String code) {
		return "{\"courtId\":\"%s\",\"startsAt\":\"%sT%s:00+05:45\",\"discountCode\":\"%s\"}".formatted(venue.courtId(),
				GAME_DAY, time, code);
	}

	private static String codes(TestVenue venue) {
		return "/api/v1/owner/venues/" + venue.id() + "/discount-codes";
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
