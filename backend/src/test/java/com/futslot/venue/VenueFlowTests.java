package com.futslot.venue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.futslot.support.IntegrationTest;
import com.futslot.support.TestUsers;
import com.futslot.support.TestUsers.TestUser;
import com.jayway.jsonpath.JsonPath;

@IntegrationTest
class VenueFlowTests {

	private static final String WEEK_HOURS = """
			[
			  {"day":"MONDAY","openTime":"06:00","closeTime":"22:00"},
			  {"day":"TUESDAY","openTime":"06:00","closeTime":"22:00"},
			  {"day":"WEDNESDAY","openTime":"06:00","closeTime":"22:00"},
			  {"day":"THURSDAY","openTime":"06:00","closeTime":"22:00"},
			  {"day":"FRIDAY","openTime":"06:00","closeTime":"23:00"},
			  {"day":"SATURDAY","openTime":"06:00","closeTime":"23:00"},
			  {"day":"SUNDAY","closed":true}
			]""";

	private static final String COURT_A = """
			{"name":"Court A","format":"FIVE_A_SIDE","surface":"ARTIFICIAL_TURF","indoor":false,"slotDurationMinutes":60}""";

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Test
	void ownerOnboardsVenueAndItGoesLiveAfterApproval() throws Exception {
		TestUser owner = this.users.signUp("Owner One");
		TestUser admin = this.users.signUpAdmin("Admin One");

		String created = send(owner, post("/api/v1/owner/venues"), venueJson("Dhuku Futsal", "Lalitpur"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("DRAFT"))
			.andExpect(jsonPath("$.slug").value(org.hamcrest.Matchers.startsWith("dhuku-futsal")))
			.andExpect(jsonPath("$.phone").value("+9779812345678"))
			.andReturn().getResponse().getContentAsString();
		String venueId = JsonPath.read(created, "$.id");
		String slug = JsonPath.read(created, "$.slug");
		String base = "/api/v1/owner/venues/" + venueId;

		// Creating a venue makes the user an owner
		this.mvc.perform(get("/api/v1/me").header("Authorization", owner.bearer()))
			.andExpect(jsonPath("$.roles", hasItem("OWNER")));

		send(owner, post(base + "/submit"), null).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VENUE_INCOMPLETE"));

		send(owner, put(base + "/operating-hours"), WEEK_HOURS).andExpect(status().isOk())
			.andExpect(jsonPath("$.operatingHours.length()").value(7))
			.andExpect(jsonPath("$.operatingHours[6].closed").value(true));
		send(owner, put(base + "/amenities"), "{\"codes\":[\"PARKING\",\"SHOWER\"]}").andExpect(status().isOk())
			.andExpect(jsonPath("$.amenities[*].code").value(org.hamcrest.Matchers.contains("PARKING", "SHOWER")));
		send(owner, put(base + "/policy"), """
				{"advancePercent":30,"bookingHoldMinutes":10,"cancellationTiers":[{"withinHours":12,"feePercent":40},{"withinHours":12,"feePercent":60}],"maxAdvanceBookingDays":14}""")
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_CANCELLATION_TIERS"));
		send(owner, put(base + "/policy"), """
				{"advancePercent":30,"bookingHoldMinutes":10,"cancellationTiers":[{"withinHours":48,"feePercent":10}],"maxAdvanceBookingDays":14}""")
			.andExpect(status().isBadRequest());
		send(owner, put(base + "/policy"), """
				{"advancePercent":30,"bookingHoldMinutes":10,"cancellationTiers":[{"withinHours":10,"feePercent":60},{"withinHours":12,"feePercent":50}],"maxAdvanceBookingDays":14}""")
			.andExpect(status().isOk()).andExpect(jsonPath("$.policy.advancePercent").value(30))
			.andExpect(jsonPath("$.policy.cancellationTiers[*].withinHours").value(org.hamcrest.Matchers.contains(12, 10)));

		String court = send(owner, post(base + "/courts"), COURT_A).andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		String courtId = JsonPath.read(court, "$.id");
		send(owner, post(base + "/courts/" + courtId + "/pricing-rules"),
				"{\"startTime\":\"06:00\",\"endTime\":\"17:00\",\"price\":1200}").andExpect(status().isCreated())
			.andExpect(jsonPath("$.label").value("STANDARD"));
		send(owner, post(base + "/courts/" + courtId + "/pricing-rules"),
				"{\"label\":\"PEAK\",\"startTime\":\"17:00\",\"endTime\":\"23:00\",\"price\":1800,\"priority\":1}")
			.andExpect(status().isCreated());

		send(owner, post(base + "/submit"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));

		// Not public until approved
		this.mvc.perform(get("/api/v1/venues/" + slug)).andExpect(status().isNotFound());

		this.mvc.perform(get("/api/v1/admin/venues").header("Authorization", admin.bearer()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].id", hasItem(venueId)));
		send(admin, post("/api/v1/admin/venues/" + venueId + "/approve"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACTIVE"));

		this.mvc.perform(get("/api/v1/venues/" + slug))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fromPrice").value(1200.0))
			.andExpect(jsonPath("$.courts.length()").value(1))
			.andExpect(jsonPath("$.courts[0].pricingRules.length()").value(2))
			.andExpect(jsonPath("$.rejectionReason").value(nullValue()));
		this.mvc.perform(get("/api/v1/venues").param("city", "lalitpur").param("q", "dhuku"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].slug", hasItem(slug)))
			.andExpect(jsonPath("$.content[0].fromPrice").value(1200.0));
	}

	@Test
	void otherUsersCannotSeeOrChangeSomeoneElsesVenue() throws Exception {
		TestUser owner = this.users.signUp("Owner Two");
		TestUser stranger = this.users.signUp("Stranger");
		String venueId = createVenue(owner, "Private Futsal");

		this.mvc.perform(get("/api/v1/owner/venues/" + venueId).header("Authorization", stranger.bearer()))
			.andExpect(status().isNotFound());
		send(stranger, post("/api/v1/owner/venues/" + venueId + "/courts"), COURT_A).andExpect(status().isNotFound());
		this.mvc.perform(get("/api/v1/owner/venues").header("Authorization", stranger.bearer()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void onlyAdminsCanModerate() throws Exception {
		TestUser owner = this.users.signUp("Owner Three");
		String venueId = createVenue(owner, "Moderation Futsal");

		send(owner, post("/api/v1/admin/venues/" + venueId + "/approve"), null).andExpect(status().isForbidden());
		this.mvc.perform(get("/api/v1/admin/venues").header("Authorization", owner.bearer()))
			.andExpect(status().isForbidden());
	}

	@Test
	void rejectedVenueShowsReasonAndCanBeResubmitted() throws Exception {
		TestUser owner = this.users.signUp("Owner Four");
		TestUser admin = this.users.signUpAdmin("Admin Two");
		String venueId = createCompleteVenue(owner, "Rejected Futsal");
		String base = "/api/v1/owner/venues/" + venueId;
		send(owner, post(base + "/submit"), null).andExpect(status().isOk());

		send(admin, post("/api/v1/admin/venues/" + venueId + "/approve"), null).andExpect(status().isOk());
		send(admin, post("/api/v1/admin/venues/" + venueId + "/reject"), "{\"reason\":\"Blurry photos\"}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INVALID_VENUE_STATUS"));
		send(admin, post("/api/v1/admin/venues/" + venueId + "/suspend"), "{\"reason\":\"Fake address\"}")
			.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED"));

		this.mvc.perform(get(base).header("Authorization", owner.bearer()))
			.andExpect(jsonPath("$.status").value("SUSPENDED"))
			.andExpect(jsonPath("$.rejectionReason").value("Fake address"));
		send(owner, post(base + "/submit"), null).andExpect(status().isConflict());
		send(admin, post("/api/v1/admin/venues/" + venueId + "/reinstate"), null)
			.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	@Test
	void ownerInputIsValidated() throws Exception {
		TestUser owner = this.users.signUp("Owner Five");
		String venueId = createVenue(owner, "Validation Futsal");
		String base = "/api/v1/owner/venues/" + venueId;

		send(owner, put(base + "/operating-hours"), "[{\"day\":\"MONDAY\",\"openTime\":\"06:00\",\"closeTime\":\"22:00\"}]")
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_HOURS"));
		send(owner, put(base + "/amenities"), "{\"codes\":[\"PARKING\",\"HELIPAD\"]}")
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("UNKNOWN_AMENITY"));

		String courtId = JsonPath.read(send(owner, post(base + "/courts"), COURT_A).andReturn().getResponse()
			.getContentAsString(), "$.id");
		send(owner, post(base + "/courts"), COURT_A.replace("Court A", "court a")).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("COURT_NAME_TAKEN"));
		send(owner, post(base + "/courts/" + courtId + "/pricing-rules"),
				"{\"startTime\":\"18:00\",\"endTime\":\"06:00\",\"price\":1500}")
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PRICING_RULE"));
		send(owner, post(base + "/courts/" + courtId + "/pricing-rules"),
				"{\"startTime\":\"06:00\",\"endTime\":\"18:00\",\"price\":-5}")
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void pricingRulesCanBeUpdatedAndDeleted() throws Exception {
		TestUser owner = this.users.signUp("Owner Six");
		String venueId = createVenue(owner, "Pricing Futsal");
		String base = "/api/v1/owner/venues/" + venueId;
		String courtId = JsonPath.read(send(owner, post(base + "/courts"), COURT_A).andReturn().getResponse()
			.getContentAsString(), "$.id");
		String rules = base + "/courts/" + courtId + "/pricing-rules";
		String ruleId = JsonPath.read(send(owner, post(rules), "{\"startTime\":\"06:00\",\"endTime\":\"12:00\",\"price\":1000}")
			.andReturn().getResponse().getContentAsString(), "$.id");

		send(owner, put(rules + "/" + ruleId), "{\"dayOfWeek\":\"SATURDAY\",\"startTime\":\"06:00\",\"endTime\":\"12:00\",\"price\":1400}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.price").value(1400))
			.andExpect(jsonPath("$.dayOfWeek").value("SATURDAY"));
		send(owner, delete(rules + "/" + ruleId), null).andExpect(status().isNoContent());
		this.mvc.perform(get(base).header("Authorization", owner.bearer()))
			.andExpect(jsonPath("$.courts[0].pricingRules.length()").value(0));
	}

	@Test
	void venuesWithTheSameNameGetDistinctSlugs() throws Exception {
		TestUser owner = this.users.signUp("Owner Seven");
		List<String> slugs = List.of(slugOf(owner, "Twin Futsal"), slugOf(owner, "Twin Futsal"));

		assertThat(slugs.get(0)).startsWith("twin-futsal");
		assertThat(slugs.get(1)).isNotEqualTo(slugs.get(0)).startsWith("twin-futsal-");
	}

	@Test
	void amenitiesAndVenueListAreReadableWithoutLogin() throws Exception {
		this.mvc.perform(get("/api/v1/amenities")).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].code", hasItem("FLOODLIGHTS")));
		this.mvc.perform(get("/api/v1/venues")).andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0));
		this.mvc.perform(get("/api/v1/owner/venues")).andExpect(status().isUnauthorized());
	}

	private String createVenue(TestUser owner, String name) throws Exception {
		String body = send(owner, post("/api/v1/owner/venues"), venueJson(name, "Kathmandu"))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	private String createCompleteVenue(TestUser owner, String name) throws Exception {
		String venueId = createVenue(owner, name);
		String base = "/api/v1/owner/venues/" + venueId;
		send(owner, put(base + "/operating-hours"), WEEK_HOURS).andExpect(status().isOk());
		String courtId = JsonPath.read(send(owner, post(base + "/courts"), COURT_A).andReturn().getResponse()
			.getContentAsString(), "$.id");
		send(owner, post(base + "/courts/" + courtId + "/pricing-rules"),
				"{\"startTime\":\"06:00\",\"endTime\":\"22:00\",\"price\":1500}").andExpect(status().isCreated());
		return venueId;
	}

	private String slugOf(TestUser owner, String name) throws Exception {
		return JsonPath.read(send(owner, post("/api/v1/owner/venues"), venueJson(name, "Kathmandu"))
			.andReturn().getResponse().getContentAsString(), "$.slug");
	}

	private static String venueJson(String name, String city) {
		return """
				{"name":"%s","description":"Two turf courts","phone":"9812345678","addressLine":"Jhamsikhel Road",
				 "area":"Jhamsikhel","city":"%s","latitude":27.679,"longitude":85.307}""".formatted(name, city);
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request, String json) throws Exception {
		request.header("Authorization", user.bearer());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request);
	}

}
