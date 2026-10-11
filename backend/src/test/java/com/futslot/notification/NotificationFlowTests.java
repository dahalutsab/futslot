package com.futslot.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.futslot.support.IntegrationTest;
import com.futslot.support.RecordingPushSender;
import com.futslot.support.TestUsers;
import com.futslot.support.TestUsers.TestUser;
import com.futslot.support.TestVenues;
import com.futslot.support.TestVenues.TestVenue;
import com.jayway.jsonpath.JsonPath;

@IntegrationTest
class NotificationFlowTests {

	private static final ZoneId KATHMANDU = ZoneId.of("Asia/Kathmandu");

	private static final LocalDate TOMORROW = LocalDate.now(KATHMANDU).plusDays(1);

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Autowired
	RecordingPushSender push;

	@Autowired
	NotificationService notificationService;

	@Autowired
	JdbcClient jdbcClient;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void appBookingNotifiesPlayerByPushAndOwnerInInbox() throws Exception {
		TestUser owner = this.users.signUp("Notify Owner");
		TestVenue venue = this.venues.active(owner, "Notify Futsal");
		TestUser player = this.users.signUp("Notify Player");
		String token = "fcm-" + UUID.randomUUID();
		send(player, post("/api/v1/me/devices"), "{\"token\":\"%s\",\"platform\":\"ANDROID\"}".formatted(token))
			.andExpect(status().isNoContent());

		String bookingId = JsonPath.read(book(player, venue, "07:00").andExpect(status().isCreated()).andReturn()
			.getResponse().getContentAsString(), "$.id");

		inbox(player).andExpect(jsonPath("$.content[0].type").value("BOOKING_CONFIRMED"))
			.andExpect(jsonPath("$.content[0].title").value("Booking confirmed"))
			.andExpect(jsonPath("$.content[0].data.bookingId").value(bookingId))
			.andExpect(jsonPath("$.content[0].read").value(false));
		assertThat(this.push.sentTo(token)).singleElement()
			.satisfies(sent -> assertThat(sent.body()).contains("Notify Futsal", "Court A", "07:00"));
		inbox(owner).andExpect(jsonPath("$.content[*].type", hasItem("NEW_BOOKING")))
			.andExpect(jsonPath("$.content[*].type", hasItem("VENUE_APPROVED")));
	}

	@Test
	void readStateIsTrackedPerUser() throws Exception {
		TestUser owner = this.users.signUp("Read Owner");
		TestVenue venue = this.venues.active(owner, "Read Futsal");
		TestUser player = this.users.signUp("Read Player");
		book(player, venue, "08:00");
		book(player, venue, "09:00");
		String first = JsonPath.read(inbox(player).andReturn().getResponse().getContentAsString(), "$.content[0].id");

		send(player, get("/api/v1/notifications/unread-count"), null).andExpect(jsonPath("$.unread").value(2));
		send(owner, post("/api/v1/notifications/" + first + "/read"), null).andExpect(status().isNotFound());
		send(player, post("/api/v1/notifications/" + first + "/read"), null).andExpect(jsonPath("$.read").value(true));
		send(player, get("/api/v1/notifications/unread-count"), null).andExpect(jsonPath("$.unread").value(1));
		send(player, post("/api/v1/notifications/read-all"), null).andExpect(jsonPath("$.updated").value(1));
		send(player, get("/api/v1/notifications/unread-count"), null).andExpect(jsonPath("$.unread").value(0));
	}

	@Test
	void failedBookingNotifiesNobody() throws Exception {
		TestVenue venue = this.venues.active(this.users.signUp("Fail Owner"), "Fail Futsal");
		book(this.users.signUp("First"), venue, "10:00").andExpect(status().isCreated());
		TestUser late = this.users.signUp("Too Late");

		book(late, venue, "10:00").andExpect(status().isConflict());

		inbox(late).andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void cancellationNotifiesTheOtherSide() throws Exception {
		TestUser owner = this.users.signUp("Cancel Notify Owner");
		TestVenue venue = this.venues.active(owner, "Cancel Notify Futsal");
		TestUser player = this.users.signUp("Cancel Notify Player");
		String byPlayer = bookingId(book(player, venue, "11:00"));
		String byOwner = bookingId(book(player, venue, "12:00"));

		send(player, post("/api/v1/bookings/" + byPlayer + "/cancel"), "{\"reason\":\"Injury\"}").andExpect(status().isOk());
		send(owner, post("/api/v1/owner/venues/" + venue.id() + "/bookings/" + byOwner + "/cancel"),
				"{\"reason\":\"Pitch repair\"}").andExpect(status().isOk());

		inbox(owner).andExpect(jsonPath("$.content[0].type").value("BOOKING_CANCELLED"))
			.andExpect(jsonPath("$.content[0].body").value(containsString("Injury")));
		inbox(player).andExpect(jsonPath("$.content[0].type").value("BOOKING_CANCELLED"))
			.andExpect(jsonPath("$.content[0].title").value("Cancel Notify Futsal cancelled your booking"))
			.andExpect(jsonPath("$.content[0].body").value(containsString("Pitch repair")));
	}

	@Test
	void walkInBookingForAnAppUserShowsUpInTheirApp() throws Exception {
		TestUser owner = this.users.signUp("Walkin Notify Owner");
		TestVenue venue = this.venues.active(owner, "Walkin Notify Futsal");
		TestUser player = this.users.signUp("Has The App");
		String phone = JsonPath.read(send(player, get("/api/v1/me"), null).andReturn().getResponse().getContentAsString(),
				"$.phone");

		String bookingId = bookingId(send(owner, post("/api/v1/owner/venues/" + venue.id() + "/bookings"), """
				{"courtId":"%s","startsAt":"%sT13:00:00+05:45","customerName":"Has The App","customerPhone":"%s","source":"PHONE"}"""
			.formatted(venue.courtId(), TOMORROW, phone)).andExpect(status().isCreated()));

		inbox(player).andExpect(jsonPath("$.content[0].type").value("BOOKING_CREATED_BY_VENUE"));
		send(player, get("/api/v1/bookings"), null).andExpect(jsonPath("$[*].id", hasItem(bookingId)));
		inbox(owner).andExpect(jsonPath("$.content[*].type", not(hasItem("NEW_BOOKING"))));
	}

	@Test
	void lapsedHoldNotifiesThePlayer() throws Exception {
		TestUser owner = this.users.signUp("Expiry Owner");
		TestVenue venue = this.venues.active(owner, "Expiry Futsal");
		send(owner, put("/api/v1/owner/venues/" + venue.id() + "/policy"), """
				{"advancePercent":20,"bookingHoldMinutes":10,"cancellationTiers":[{"withinHours":24,"feePercent":100}],"maxAdvanceBookingDays":30}""");
		TestUser slow = this.users.signUp("Slow");
		String held = bookingId(book(slow, venue, "14:00"));
		inbox(slow).andExpect(jsonPath("$.content[0].type").value("BOOKING_PAYMENT_DUE"))
			.andExpect(jsonPath("$.content[0].body").value(containsString("Rs 240")));
		this.jdbc.update("UPDATE bookings SET hold_expires_at = now() - interval '1 second' WHERE id = ?::uuid", held);

		book(this.users.signUp("Quick"), venue, "14:00").andExpect(status().isCreated());

		inbox(slow).andExpect(jsonPath("$.content[0].type").value("BOOKING_EXPIRED"));
	}

	@Test
	void reminderIsSentOnceShortlyBeforeTheGame() throws Exception {
		TestUser owner = this.users.signUp("Remind Owner");
		TestVenue venue = this.venues.active(owner, "Remind Futsal");
		TestUser player = this.users.signUp("Remind Player");
		OffsetDateTime soon = OffsetDateTime.now(KATHMANDU).plusMinutes(90).truncatedTo(ChronoUnit.MINUTES);
		String bookingId = bookingId(send(owner, post("/api/v1/owner/venues/" + venue.id() + "/bookings"), """
				{"courtId":"%s","startsAt":"%s","endsAt":"%s","customerName":"Remind Player","customerPhone":"%s",
				 "source":"PHONE","price":1500}""".formatted(venue.courtId(), soon, soon.plusHours(1), phoneOf(player)))
			.andExpect(status().isCreated()));
		BookingReminders reminders = new BookingReminders(this.jdbcClient, this.notificationService,
				Duration.ofHours(2), Clock.systemUTC());

		assertThat(reminders.run()).isGreaterThanOrEqualTo(1);
		assertThat(reminders.run()).isZero();

		List<String> types = JsonPath.read(inbox(player).andReturn().getResponse().getContentAsString(), "$.content[*].type");
		assertThat(types).containsOnlyOnce("BOOKING_REMINDER");
		assertThat(this.jdbc.queryForObject("SELECT reminder_sent_at IS NOT NULL FROM bookings WHERE id = ?::uuid",
				Boolean.class, bookingId)).isTrue();
	}

	@Test
	void unregisteredDeviceStopsReceivingPushes() throws Exception {
		TestVenue venue = this.venues.active(this.users.signUp("Device Owner"), "Device Futsal");
		TestUser player = this.users.signUp("Device Player");
		String token = "fcm-" + UUID.randomUUID();
		send(player, post("/api/v1/me/devices"), "{\"token\":\"%s\",\"platform\":\"IOS\"}".formatted(token));
		send(player, delete("/api/v1/me/devices/" + token), null).andExpect(status().isNoContent());

		book(player, venue, "15:00").andExpect(status().isCreated());

		assertThat(this.push.sentTo(token)).isEmpty();
		inbox(player).andExpect(jsonPath("$.content[0].type").value("BOOKING_CONFIRMED"));
	}

	private String phoneOf(TestUser user) throws Exception {
		return JsonPath.read(send(user, get("/api/v1/me"), null).andReturn().getResponse().getContentAsString(), "$.phone");
	}

	private static String bookingId(ResultActions result) throws Exception {
		return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
	}

	private ResultActions inbox(TestUser user) throws Exception {
		return send(user, get("/api/v1/notifications"), null).andExpect(status().isOk());
	}

	private ResultActions book(TestUser player, TestVenue venue, String time) throws Exception {
		return send(player, post("/api/v1/bookings"),
				"{\"courtId\":\"%s\",\"startsAt\":\"%sT%s:00+05:45\"}".formatted(venue.courtId(), TOMORROW, time));
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request, String json) throws Exception {
		request.header("Authorization", user.bearer());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request);
	}

}
