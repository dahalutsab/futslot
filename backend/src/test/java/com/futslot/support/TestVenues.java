package com.futslot.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.futslot.support.TestUsers.TestUser;
import com.jayway.jsonpath.JsonPath;

/**
 * Creates a fully set-up venue through the API: open 06:00-22:00 every day, one hourly court priced
 * Rs 1200 until 17:00 and Rs 1800 (PEAK) after.
 */
public class TestVenues {

	private static final String EVERY_DAY_6_TO_22 = """
			[{"day":"MONDAY","openTime":"06:00","closeTime":"22:00"},{"day":"TUESDAY","openTime":"06:00","closeTime":"22:00"},
			 {"day":"WEDNESDAY","openTime":"06:00","closeTime":"22:00"},{"day":"THURSDAY","openTime":"06:00","closeTime":"22:00"},
			 {"day":"FRIDAY","openTime":"06:00","closeTime":"22:00"},{"day":"SATURDAY","openTime":"06:00","closeTime":"22:00"},
			 {"day":"SUNDAY","openTime":"06:00","closeTime":"22:00"}]""";

	private final MockMvc mvc;

	private final TestUsers users;

	TestVenues(MockMvc mvc, TestUsers users) {
		this.mvc = mvc;
		this.users = users;
	}

	/** Set up but still a DRAFT, so only its owner can see it. */
	public TestVenue draft(TestUser owner, String name) throws Exception {
		String venue = body(owner, post("/api/v1/owner/venues"), """
				{"name":"%s","addressLine":"Test Road","city":"Kathmandu"}""".formatted(name));
		String venueId = JsonPath.read(venue, "$.id");
		String base = "/api/v1/owner/venues/" + venueId;
		body(owner, put(base + "/operating-hours"), EVERY_DAY_6_TO_22);
		String court = body(owner, post(base + "/courts"),
				"{\"name\":\"Court A\",\"format\":\"FIVE_A_SIDE\",\"surface\":\"ARTIFICIAL_TURF\",\"slotDurationMinutes\":60}");
		String courtId = JsonPath.read(court, "$.id");
		String rules = base + "/courts/" + courtId + "/pricing-rules";
		body(owner, post(rules), "{\"startTime\":\"06:00\",\"endTime\":\"17:00\",\"price\":1200}");
		body(owner, post(rules), "{\"label\":\"PEAK\",\"startTime\":\"17:00\",\"endTime\":\"22:00\",\"price\":1800,\"priority\":1}");
		return new TestVenue(UUID.fromString(venueId), JsonPath.read(venue, "$.slug"), UUID.fromString(courtId));
	}

	/** Set up, submitted and approved by a fresh admin: publicly visible and bookable. */
	public TestVenue active(TestUser owner, String name) throws Exception {
		TestVenue venue = draft(owner, name);
		body(owner, post("/api/v1/owner/venues/" + venue.id() + "/submit"), null);
		body(this.users.signUpAdmin("Approver"), post("/api/v1/admin/venues/" + venue.id() + "/approve"), null);
		return venue;
	}

	private String body(TestUser user, MockHttpServletRequestBuilder request, String json) throws Exception {
		request.header("Authorization", user.bearer());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request).andExpect(status().is2xxSuccessful()).andReturn().getResponse()
			.getContentAsString();
	}

	public record TestVenue(UUID id, String slug, UUID courtId) {

	}

}
