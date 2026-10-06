package com.futslot.slot;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
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
class AvailabilityFlowTests {

	private static final LocalDate TOMORROW = LocalDate.now(ZoneId.of("Asia/Kathmandu")).plusDays(1);

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void playersSeePricedSlotsForAnActiveVenue() throws Exception {
		TestVenue venue = this.venues.active(this.users.signUp("Avail Owner"), "Avail Futsal");

		availability(venue, TOMORROW).andExpect(status().isOk())
			.andExpect(jsonPath("$.timezone").value("Asia/Kathmandu"))
			.andExpect(jsonPath("$.courts[0].slots.length()").value(16))
			.andExpect(jsonPath("$.courts[0].slots[0].startsAt").value(TOMORROW + "T06:00:00+05:45"))
			.andExpect(jsonPath("$.courts[0].slots[0].price").value(1200))
			.andExpect(jsonPath("$.courts[0].slots[11].label").value("PEAK"))
			.andExpect(jsonPath("$.courts[0].slots[*].status", everyItem(is("AVAILABLE"))));
	}

	@Test
	void bookingsMarkSlotsBookedUntilTheirHoldLapses() throws Exception {
		TestVenue venue = this.venues.active(this.users.signUp("Booked Owner"), "Booked Futsal");
		insertBooking(venue, "18:00", "19:00", "CONFIRMED", null);
		insertBooking(venue, "19:00", "20:00", "PENDING_PAYMENT", "now() - interval '1 minute'");
		insertBooking(venue, "20:00", "21:00", "PENDING_PAYMENT", "now() + interval '10 minutes'");
		insertBooking(venue, "21:00", "22:00", "CANCELLED", null);

		availability(venue, TOMORROW)
			.andExpect(jsonPath("$.courts[0].slots[12].status").value("BOOKED"))
			.andExpect(jsonPath("$.courts[0].slots[13].status").value("AVAILABLE"))
			.andExpect(jsonPath("$.courts[0].slots[14].status").value("BOOKED"))
			.andExpect(jsonPath("$.courts[0].slots[15].status").value("AVAILABLE"));
	}

	@Test
	void ownerBlocksTimeAndCannotDoubleBlockOrBlockOverBookings() throws Exception {
		TestUser owner = this.users.signUp("Block Owner");
		TestVenue venue = this.venues.active(owner, "Block Futsal");
		String blocks = "/api/v1/owner/venues/" + venue.id() + "/courts/" + venue.courtId() + "/blocks";
		insertBooking(venue, "18:00", "19:00", "CONFIRMED", null);

		String created = send(owner, post(blocks), block("06:00", "08:00", "MAINTENANCE")).andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		send(owner, post(blocks), block("07:00", "09:00", "OTHER")).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("BLOCK_OVERLAP"));
		send(owner, post(blocks), block("17:30", "18:30", "PRIVATE_EVENT")).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SLOT_HAS_BOOKINGS"));

		availability(venue, TOMORROW).andExpect(jsonPath("$.courts[0].slots[0].status").value("BLOCKED"))
			.andExpect(jsonPath("$.courts[0].slots[1].status").value("BLOCKED"))
			.andExpect(jsonPath("$.courts[0].slots[2].status").value("AVAILABLE"));

		this.mvc.perform(get(blocks).header("Authorization", owner.bearer())
			.param("from", TOMORROW + "T00:00:00+05:45").param("to", TOMORROW.plusDays(1) + "T00:00:00+05:45"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

		send(owner, delete(blocks + "/" + JsonPath.read(created, "$.id")), null).andExpect(status().isNoContent());
		availability(venue, TOMORROW).andExpect(jsonPath("$.courts[0].slots[0].status").value("AVAILABLE"));
	}

	@Test
	void datesOutsideTheBookingWindowAreRejected() throws Exception {
		TestVenue venue = this.venues.active(this.users.signUp("Window Owner"), "Window Futsal");

		availability(venue, TOMORROW.minusDays(2)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("DATE_OUT_OF_RANGE"));
		availability(venue, TOMORROW.plusDays(60)).andExpect(status().isBadRequest());
	}

	@Test
	void draftVenueIsHiddenFromPlayersButVisibleToItsOwner() throws Exception {
		TestUser owner = this.users.signUp("Draft Owner");
		TestVenue venue = this.venues.draft(owner, "Draft Futsal");

		availability(venue, TOMORROW).andExpect(status().isNotFound());
		this.mvc.perform(get("/api/v1/owner/venues/" + venue.id() + "/availability").param("date", TOMORROW.toString())
			.header("Authorization", owner.bearer()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.courts[0].slots.length()").value(16));
		this.mvc.perform(get("/api/v1/owner/venues/" + venue.id() + "/availability").param("date", TOMORROW.toString())
			.header("Authorization", this.users.signUp("Nosy").bearer()))
			.andExpect(status().isNotFound());
	}

	private ResultActions availability(TestVenue venue, LocalDate date) throws Exception {
		return this.mvc.perform(get("/api/v1/venues/" + venue.slug() + "/availability").param("date", date.toString()));
	}

	private void insertBooking(TestVenue venue, String start, String end, String status, String holdExpiresSql) {
		UUID customerId = this.jdbc.queryForObject("""
				INSERT INTO venue_customers (venue_id, name, phone) VALUES (?, 'Guest', ?) RETURNING id""",
				UUID.class, venue.id(), "+97798" + (System.nanoTime() % 100_000_000L));
		this.jdbc.update("""
				INSERT INTO bookings (reference, venue_id, court_id, customer_id, source, status, starts_at, ends_at,
				                      price_amount, total_amount, hold_expires_at, cancelled_at)
				VALUES (?, ?, ?, ?, 'PHONE', ?, ?::timestamptz, ?::timestamptz, 1800, 1800, %s, %s)"""
			.formatted(holdExpiresSql != null ? holdExpiresSql : "NULL", "CANCELLED".equals(status) ? "now()" : "NULL"),
				"T-" + UUID.randomUUID(), venue.id(), venue.courtId(), customerId, status,
				TOMORROW + "T" + start + "+05:45", TOMORROW + "T" + end + "+05:45");
	}

	private static String block(String start, String end, String reason) {
		return """
				{"startsAt":"%sT%s:00+05:45","endsAt":"%sT%s:00+05:45","reason":"%s"}""".formatted(TOMORROW, start, TOMORROW,
				end, reason);
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request,
			String json) throws Exception {
		request.header("Authorization", user.bearer());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request);
	}

}
