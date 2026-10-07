package com.futslot.membership;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

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
class MembershipFlowTests {

	private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Kathmandu"));

	private static final LocalDate GAME_DAY = TODAY.plusDays(3);

	private static final String TWO_GAME_PACK = """
			{"name":"2-game pack","type":"GAME_PACK","gamesIncluded":2,"validityDays":30,"price":2000}""";

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Test
	void playerBuysAGamePackAndPlaysWithIt() throws Exception {
		TestUser owner = this.users.signUp("Pack Owner");
		TestVenue venue = advanceVenue(owner, "Pack Futsal");
		send(owner, post(venuePath(venue) + "/membership-plans"),
				"{\"name\":\"Broken\",\"type\":\"GAME_PACK\",\"validityDays\":30,\"price\":2000}")
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PLAN"));
		String planId = id(send(owner, post(venuePath(venue) + "/membership-plans"), TWO_GAME_PACK)
			.andExpect(status().isCreated()));
		this.mvc.perform(get("/api/v1/venues/" + venue.slug() + "/membership-plans"))
			.andExpect(jsonPath("$[0].name").value("2-game pack"));

		TestUser player = this.users.signUp("Pack Player");
		send(player, post("/api/v1/me/memberships"), buy(planId, 1500)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("AMOUNT_MISMATCH"));
		String membershipId = id(send(player, post("/api/v1/me/memberships"), buy(planId, 2000))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("PENDING_PAYMENT")));
		send(player, post("/api/v1/bookings"), booking(venue, "07:00", membershipId)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MEMBERSHIP_NOT_ACTIVE"));

		String paymentId = JsonPath.read(send(owner, get(venuePath(venue) + "/payments/pending"), null)
			.andExpect(jsonPath("$[0].purpose").value("MEMBERSHIP"))
			.andExpect(jsonPath("$[0].membershipPlanName").value("2-game pack"))
			.andReturn().getResponse().getContentAsString(), "$[0].id");
		send(owner, post(venuePath(venue) + "/payments/" + paymentId + "/approve"), null).andExpect(status().isOk());
		send(player, get("/api/v1/me/memberships"), null)
			.andExpect(jsonPath("$[0].status").value("ACTIVE"))
			.andExpect(jsonPath("$[0].startsOn").value(TODAY.toString()))
			.andExpect(jsonPath("$[0].expiresOn").value(TODAY.plusDays(29).toString()))
			.andExpect(jsonPath("$[0].gamesRemaining").value(2));
		send(player, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("MEMBERSHIP_ACTIVATED"));

		// Each game is covered in full, so there's nothing to pay and no hold
		String first = id(send(player, post("/api/v1/bookings"), booking(venue, "07:00", membershipId))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("CONFIRMED"))
			.andExpect(jsonPath("$.discountAmount").value(1200.0))
			.andExpect(jsonPath("$.totalAmount").value(0.0))
			.andExpect(jsonPath("$.paymentStatus").value("PAID"))
			.andExpect(jsonPath("$.membershipId").value(membershipId)));
		send(player, post("/api/v1/bookings"), booking(venue, "18:00", membershipId)).andExpect(status().isCreated());
		send(player, post("/api/v1/bookings"), booking(venue, "19:00", membershipId)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MEMBERSHIP_USED_UP"));

		// Cancelled in good time: the game comes back
		send(player, post("/api/v1/bookings/" + first + "/cancel"), "{}").andExpect(status().isOk());
		send(player, get("/api/v1/me/memberships"), null).andExpect(jsonPath("$[0].gamesRemaining").value(1));
		send(this.users.signUp("Freeloader"), post("/api/v1/bookings"), booking(venue, "08:00", membershipId))
			.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("MEMBERSHIP_NOT_FOUND"));
	}

	@Test
	void staffSellAPassAtTheCounterThatTakesAPercentageOff() throws Exception {
		TestUser owner = this.users.signUp("Pass Owner");
		TestVenue venue = advanceVenue(owner, "Pass Futsal");
		String planId = id(send(owner, post(venuePath(venue) + "/membership-plans"), """
				{"name":"Monthly 20% off","type":"MONTHLY_PASS","discountPercent":20,"validityDays":30,"price":1000}"""));
		String phone = TestUsers.nextLocalPhone();
		TestUser player = this.users.logIn(phone, "Pass Player");

		String membershipId = id(send(owner, post(venuePath(venue) + "/memberships"), """
				{"planId":"%s","customerName":"Pass Player","customerPhone":"%s","method":"CASH"}""".formatted(planId, phone))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.gamesRemaining").doesNotExist()));
		send(owner, get(venuePath(venue) + "/memberships").param("status", "ACTIVE"), null)
			.andExpect(jsonPath("$.content[*].id", hasItem(membershipId)));

		send(player, post("/api/v1/bookings/quote"), booking(venue, "07:00", membershipId)).andExpect(status().isOk())
			.andExpect(jsonPath("$.price").value(1200.0))
			.andExpect(jsonPath("$.discount").value(240.0))
			.andExpect(jsonPath("$.total").value(960.0))
			.andExpect(jsonPath("$.advanceRequired").value(288.0));
		send(player, post("/api/v1/bookings"), booking(venue, "07:00", membershipId)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
			.andExpect(jsonPath("$.totalAmount").value(960.0))
			.andExpect(jsonPath("$.advanceRequired").value(288.0));
		send(player, post("/api/v1/bookings"), """
				{"courtId":"%s","startsAt":"%sT08:00:00+05:45","membershipId":"%s","discountCode":"ANY"}"""
			.formatted(venue.courtId(), GAME_DAY, membershipId)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("ONE_DISCOUNT_ONLY"));
		send(player, post("/api/v1/bookings"), """
				{"courtId":"%s","startsAt":"%sT08:00:00+05:45","membershipId":"%s"}"""
			.formatted(venue.courtId(), TODAY.plusDays(30), membershipId)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MEMBERSHIP_NOT_VALID_ON_DATE"));

		send(owner, post(venuePath(venue) + "/memberships/" + membershipId + "/cancel"), null)
			.andExpect(jsonPath("$.status").value("CANCELLED"));
		send(player, post("/api/v1/bookings"), booking(venue, "09:00", membershipId)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MEMBERSHIP_NOT_ACTIVE"));
	}

	@Test
	void soldPlansKeepTheirBenefitsAndRejectedPurchasesAreCancelled() throws Exception {
		TestUser owner = this.users.signUp("Plan Owner");
		TestVenue venue = advanceVenue(owner, "Plan Futsal");
		String planId = id(send(owner, post(venuePath(venue) + "/membership-plans"), TWO_GAME_PACK));
		TestUser player = this.users.signUp("Plan Player");
		String membershipId = id(send(player, post("/api/v1/me/memberships"), buy(planId, 2000)));
		String paymentId = JsonPath.read(send(owner, get(venuePath(venue) + "/payments/pending"), null).andReturn()
			.getResponse().getContentAsString(), "$[0].id");

		send(owner, put(venuePath(venue) + "/membership-plans/" + planId), TWO_GAME_PACK.replace("\"gamesIncluded\":2",
				"\"gamesIncluded\":5")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PLAN_IN_USE"));
		send(owner, put(venuePath(venue) + "/membership-plans/" + planId),
				TWO_GAME_PACK.replace("2-game pack", "Duo pack").replace("2000", "1800"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Duo pack"));

		send(owner, post(venuePath(venue) + "/payments/" + paymentId + "/reject"), "{\"reason\":\"Not received\"}")
			.andExpect(status().isOk());
		send(player, get("/api/v1/me/memberships"), null).andExpect(jsonPath("$[0].id").value(membershipId))
			.andExpect(jsonPath("$[0].status").value("CANCELLED"));
		send(player, get("/api/v1/notifications"), null).andExpect(jsonPath("$.content[0].type").value("PAYMENT_REJECTED"));
	}

	private TestVenue advanceVenue(TestUser owner, String name) throws Exception {
		TestVenue venue = this.venues.active(owner, name);
		send(owner, put(venuePath(venue) + "/policy"), """
				{"advancePercent":30,"bookingHoldMinutes":15,"freeCancelHours":24,"lateCancelRefundPct":0,"maxAdvanceBookingDays":30}""")
			.andExpect(status().isOk());
		return venue;
	}

	private static String buy(String planId, int amount) {
		return "{\"planId\":\"%s\",\"method\":\"ESEWA\",\"transactionId\":\"ES-%s\",\"amount\":%d}".formatted(planId,
				UUID.randomUUID(), amount);
	}

	private static String booking(TestVenue venue, String time, String membershipId) {
		return "{\"courtId\":\"%s\",\"startsAt\":\"%sT%s:00+05:45\",\"membershipId\":\"%s\"}".formatted(venue.courtId(),
				GAME_DAY, time, membershipId);
	}

	private static String venuePath(TestVenue venue) {
		return "/api/v1/owner/venues/" + venue.id();
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
