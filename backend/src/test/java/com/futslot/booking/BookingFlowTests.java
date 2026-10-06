package com.futslot.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import com.futslot.common.ApiException;
import com.futslot.support.IntegrationTest;
import com.futslot.support.TestUsers;
import com.futslot.support.TestUsers.TestUser;
import com.futslot.support.TestVenues;
import com.futslot.support.TestVenues.TestVenue;
import com.jayway.jsonpath.JsonPath;

@IntegrationTest
class BookingFlowTests {

	private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Kathmandu"));

	private static final LocalDate TOMORROW = TODAY.plusDays(1);

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Autowired
	BookingService bookingService;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	JdbcClient jdbcClient;

	@Autowired
	HoldExpiry holdExpiry;

	@Autowired
	TransactionTemplate transactions;

	@Test
	void playerBooksAFreeSlotAndOthersCannotTakeIt() throws Exception {
		TestUser owner = this.users.signUp("Book Owner");
		TestVenue venue = this.venues.active(owner, "Book Futsal");
		TestUser player = this.users.signUp("Player One");

		String body = book(player, venue, "07:00").andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("CONFIRMED"))
			.andExpect(jsonPath("$.reference").value(matchesPattern("FS-[2-9A-HJ-NP-Z]{6}")))
			.andExpect(jsonPath("$.totalAmount").value(1200))
			.andExpect(jsonPath("$.startsAt").value(TOMORROW + "T07:00:00+05:45"))
			.andExpect(jsonPath("$.endsAt").value(TOMORROW + "T08:00:00+05:45"))
			.andExpect(jsonPath("$.customer.name").value("Player One"))
			.andReturn().getResponse().getContentAsString();
		String bookingId = JsonPath.read(body, "$.id");

		book(this.users.signUp("Player Two"), venue, "07:00").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));

		this.mvc.perform(get("/api/v1/venues/" + venue.slug() + "/availability").param("date", TOMORROW.toString()))
			.andExpect(jsonPath("$.courts[0].slots[1].status").value("BOOKED"));
		send(player, get("/api/v1/bookings"), null).andExpect(jsonPath("$[*].id", hasItem(bookingId)));
		send(owner, get("/api/v1/owner/venues/" + venue.id() + "/bookings").param("date", TOMORROW.toString()), null)
			.andExpect(jsonPath("$[*].id", hasItem(bookingId)));
		assertThat(this.jdbc.queryForObject("SELECT user_id FROM venue_customers WHERE id = (SELECT customer_id FROM bookings WHERE id = ?::uuid)",
				UUID.class, bookingId)).isEqualTo(player.id());
	}

	@Test
	void venueWithAdvanceHoldsSlotUntilPaymentAndLimitsHolds() throws Exception {
		TestUser owner = this.users.signUp("Advance Owner");
		TestVenue venue = this.venues.active(owner, "Advance Futsal");
		send(owner, put("/api/v1/owner/venues/" + venue.id() + "/policy"), """
				{"advancePercent":30,"bookingHoldMinutes":10,"freeCancelHours":24,"lateCancelRefundPct":0,"maxAdvanceBookingDays":30}""")
			.andExpect(status().isOk());
		TestUser player = this.users.signUp("Hold Player");

		book(player, venue, "06:00").andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
			.andExpect(jsonPath("$.advanceRequired").value(360.0))
			.andExpect(jsonPath("$.holdExpiresAt").value(notNullValue()));
		book(player, venue, "07:00").andExpect(status().isCreated());
		book(player, venue, "08:00").andExpect(status().isCreated());
		book(player, venue, "09:00").andExpect(status().isTooManyRequests())
			.andExpect(jsonPath("$.code").value("TOO_MANY_HOLDS"));
	}

	@Test
	void lapsedHoldIsExpiredAndTheSlotCanBeRebooked() throws Exception {
		TestUser owner = this.users.signUp("Lapse Owner");
		TestVenue venue = this.venues.active(owner, "Lapse Futsal");
		send(owner, put("/api/v1/owner/venues/" + venue.id() + "/policy"), """
				{"advancePercent":50,"bookingHoldMinutes":10,"freeCancelHours":24,"lateCancelRefundPct":0,"maxAdvanceBookingDays":30}""");
		String first = JsonPath.read(book(this.users.signUp("Slow Payer"), venue, "10:00").andReturn().getResponse()
			.getContentAsString(), "$.id");
		this.jdbc.update("UPDATE bookings SET hold_expires_at = now() - interval '1 second' WHERE id = ?::uuid", first);

		book(this.users.signUp("Fast Payer"), venue, "10:00").andExpect(status().isCreated());
		assertThat(this.jdbc.queryForObject("SELECT status FROM bookings WHERE id = ?::uuid", String.class, first))
			.isEqualTo("EXPIRED");
	}

	@Test
	void onlyRealFutureSlotsInsideTheWindowCanBeBooked() throws Exception {
		TestVenue venue = this.venues.active(this.users.signUp("Rules Owner"), "Rules Futsal");
		TestUser player = this.users.signUp("Rules Player");

		book(player, venue, TOMORROW, "06:30").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("SLOT_NOT_FOUND"));
		book(player, venue, TOMORROW, "23:00").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("SLOT_NOT_FOUND"));
		book(player, venue, TODAY.minusDays(1), "10:00").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SLOT_IN_PAST"));
		book(player, venue, TODAY.plusDays(45), "10:00").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("DATE_OUT_OF_RANGE"));
	}

	@Test
	void playerCancelsOwnBookingWhichFreesTheSlot() throws Exception {
		TestVenue venue = this.venues.active(this.users.signUp("Cancel Owner"), "Cancel Futsal");
		TestUser player = this.users.signUp("Cancel Player");
		String bookingId = JsonPath.read(book(player, venue, "12:00").andReturn().getResponse().getContentAsString(), "$.id");

		send(this.users.signUp("Other Player"), post("/api/v1/bookings/" + bookingId + "/cancel"), "{}")
			.andExpect(status().isNotFound());
		send(player, post("/api/v1/bookings/" + bookingId + "/cancel"), "{\"reason\":\"Rain\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"))
			.andExpect(jsonPath("$.cancellationReason").value("Rain"));
		send(player, post("/api/v1/bookings/" + bookingId + "/cancel"), null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INVALID_BOOKING_STATUS"));
		book(this.users.signUp("Next Player"), venue, "12:00").andExpect(status().isCreated());
	}

	@Test
	void ownerEntersWalkInsPricedFromSlotsOrManually() throws Exception {
		TestUser owner = this.users.signUp("Walkin Owner");
		TestVenue venue = this.venues.active(owner, "Walkin Futsal");
		String bookings = "/api/v1/owner/venues/" + venue.id() + "/bookings";

		send(owner, post(bookings), manual(venue, "16:00", "18:00", null, "981-111-2222")).andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("CONFIRMED"))
			.andExpect(jsonPath("$.source").value("WALK_IN"))
			.andExpect(jsonPath("$.totalAmount").value(3000))
			.andExpect(jsonPath("$.customer.phone").value("+9779811112222"));
		send(owner, post(bookings), manual(venue, "18:30", "19:30", null, "9811112222")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("PRICE_REQUIRED"));
		send(owner, post(bookings), manual(venue, "18:30", "19:30", 1500, "9811112222")).andExpect(status().isCreated())
			.andExpect(jsonPath("$.totalAmount").value(1500));
		send(owner, post(bookings), manual(venue, "17:30", "18:30", 1000, "9800000000")).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));

		assertThat(this.jdbc.queryForObject("SELECT total_bookings FROM venue_customers WHERE venue_id = ? AND phone = ?",
				Integer.class, venue.id(), "+9779811112222")).isEqualTo(2);
		send(this.users.signUp("Not Owner"), post(bookings), manual(venue, "06:00", "07:00", null, "9811112222"))
			.andExpect(status().isNotFound());
	}

	@Test
	void ownerReschedulesIntoFreeTimeIncludingOverlappingItself() throws Exception {
		TestUser owner = this.users.signUp("Move Owner");
		TestVenue venue = this.venues.active(owner, "Move Futsal");
		String bookings = "/api/v1/owner/venues/" + venue.id() + "/bookings";
		String moving = JsonPath.read(send(owner, post(bookings), manual(venue, "08:00", "09:00", null, "9811110001"))
			.andReturn().getResponse().getContentAsString(), "$.id");
		send(owner, post(bookings), manual(venue, "10:00", "11:00", null, "9811110002")).andExpect(status().isCreated());

		send(owner, post(bookings + "/" + moving + "/reschedule"), "{\"startsAt\":\"%sT08:30:00+05:45\"}".formatted(TOMORROW))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.startsAt").value(TOMORROW + "T08:30:00+05:45"))
			.andExpect(jsonPath("$.endsAt").value(TOMORROW + "T09:30:00+05:45"));
		send(owner, post(bookings + "/" + moving + "/reschedule"), "{\"startsAt\":\"%sT09:30:00+05:45\"}".formatted(TOMORROW))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
	}

	@Test
	void noShowAndCompletionOnlyAfterTheBookingStarts() throws Exception {
		TestUser owner = this.users.signUp("Status Owner");
		TestVenue venue = this.venues.active(owner, "Status Futsal");
		String bookings = "/api/v1/owner/venues/" + venue.id() + "/bookings";
		String future = JsonPath.read(send(owner, post(bookings), manual(venue, "06:00", "07:00", null, "9811110003"))
			.andReturn().getResponse().getContentAsString(), "$.id");
		String past = JsonPath.read(send(owner, post(bookings), manual(venue, TODAY.minusDays(1), "06:00", "07:00", "9811110004"))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");

		send(owner, post(bookings + "/" + future + "/no-show"), null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("BOOKING_NOT_STARTED"));
		send(owner, post(bookings + "/" + past + "/no-show"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("NO_SHOW"));
		send(owner, post(bookings + "/" + past + "/complete"), null).andExpect(status().isConflict());
	}

	@Test
	void concurrentRequestsForTheSameSlotProduceExactlyOneBooking() throws Exception {
		TestVenue venue = this.venues.active(this.users.signUp("Race Owner"), "Race Futsal");
		List<UUID> players = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			players.add(this.users.signUp("Racer " + i).id());
		}
		PlayerBookingRequest request = new PlayerBookingRequest(venue.courtId(),
				OffsetDateTime.parse(TOMORROW + "T20:00:00+05:45"), null, null, null);

		CountDownLatch start = new CountDownLatch(1);
		List<Callable<String>> attempts = players.stream().<Callable<String>>map(playerId -> () -> {
			start.await();
			try {
				this.bookingService.bookAsPlayer(playerId, request);
				return "BOOKED";
			}
			catch (ApiException ex) {
				return ex.getCode();
			}
		}).toList();
		List<String> outcomes = new ArrayList<>();
		try (ExecutorService pool = Executors.newFixedThreadPool(players.size())) {
			List<Future<String>> futures = attempts.stream().map(pool::submit).toList();
			start.countDown();
			for (Future<String> future : futures) {
				outcomes.add(future.get());
			}
		}

		assertThat(outcomes).containsOnlyOnce("BOOKED").filteredOn(outcome -> !outcome.equals("BOOKED"))
			.containsOnly("SLOT_UNAVAILABLE");
		assertThat(this.jdbc.queryForObject("SELECT count(*) FROM bookings WHERE court_id = ? AND status <> 'CANCELLED'",
				Integer.class, venue.courtId())).isEqualTo(1);
	}

	@Test
	void maintenanceCompletesFinishedBookingsAndExpiresLapsedHolds() throws Exception {
		TestUser owner = this.users.signUp("Job Owner");
		TestVenue venue = this.venues.active(owner, "Job Futsal");
		String finished = JsonPath.read(send(owner, post("/api/v1/owner/venues/" + venue.id() + "/bookings"),
				manual(venue, TODAY.minusDays(1), "09:00", "10:00", "9811110005")).andReturn().getResponse()
			.getContentAsString(), "$.id");
		String lapsed = JsonPath.read(book(this.users.signUp("Job Player"), venue, "15:00").andReturn().getResponse()
			.getContentAsString(), "$.id");
		this.jdbc.update("UPDATE bookings SET status = 'PENDING_PAYMENT', hold_expires_at = now() - interval '1 minute' WHERE id = ?::uuid", lapsed);

		// The scheduled bean is disabled in tests; run the job the way Spring would, inside a transaction
		BookingMaintenance maintenance = new BookingMaintenance(this.jdbcClient, this.holdExpiry, Clock.systemUTC());
		this.transactions.executeWithoutResult(status -> maintenance.run());

		assertThat(statusOf(finished)).isEqualTo("COMPLETED");
		assertThat(statusOf(lapsed)).isEqualTo("EXPIRED");
	}

	private String statusOf(String bookingId) {
		return this.jdbc.queryForObject("SELECT status FROM bookings WHERE id = ?::uuid", String.class, bookingId);
	}

	private ResultActions book(TestUser player, TestVenue venue, String time) throws Exception {
		return book(player, venue, TOMORROW, time);
	}

	private ResultActions book(TestUser player, TestVenue venue, LocalDate date, String time) throws Exception {
		return send(player, post("/api/v1/bookings"),
				"{\"courtId\":\"%s\",\"startsAt\":\"%sT%s:00+05:45\"}".formatted(venue.courtId(), date, time));
	}

	private static String manual(TestVenue venue, String start, String end, Integer price, String phone) {
		return """
				{"courtId":"%s","startsAt":"%sT%s:00+05:45","endsAt":"%sT%s:00+05:45","customerName":"Walk In",
				 "customerPhone":"%s","source":"WALK_IN"%s}""".formatted(venue.courtId(), TOMORROW, start, TOMORROW, end,
				phone, (price != null) ? ",\"price\":" + price : "");
	}

	private static String manual(TestVenue venue, LocalDate date, String start, String end, String phone) {
		return """
				{"courtId":"%s","startsAt":"%sT%s:00+05:45","endsAt":"%sT%s:00+05:45","customerName":"Walk In",
				 "customerPhone":"%s","source":"PHONE"}""".formatted(venue.courtId(), date, start, date, end, phone);
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request, String json) throws Exception {
		request.header("Authorization", user.bearer());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request);
	}

}
