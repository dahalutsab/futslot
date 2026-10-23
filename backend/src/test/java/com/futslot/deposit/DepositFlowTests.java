package com.futslot.deposit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
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
class DepositFlowTests {

	private static final LocalDate GAME_DAY = LocalDate.now(ZoneId.of("Asia/Kathmandu")).plusDays(3);

	private static final String RISING_FEES = "[{\"withinHours\":24,\"feePercent\":20},{\"withinHours\":12,\"feePercent\":50}]";

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PayoutService payouts;

	@Test
	void aVenueRequiringTheDepositOnlyTakesBookingsOnceItIsFull() throws Exception {
		TestUser owner = this.users.signUp("Deposit Owner");
		TestVenue venue = depositVenue(owner, "Deposit Futsal", 0);
		TestUser player = this.users.signUp("Deposit Player");

		book(player, venue, "07:00").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DEPOSIT_REQUIRED"))
			.andExpect(jsonPath("$.detail").value(containsString("Add Rs 1,000")));
		send(player, post("/api/v1/bookings/quote"),
				"{\"courtId\":\"%s\",\"startsAt\":\"%sT07:00:00+05:45\"}".formatted(venue.courtId(), GAME_DAY))
			.andExpect(jsonPath("$.depositRequired").value(true))
			.andExpect(jsonPath("$.depositToAdd").value(1000.0));

		topUp(player, 500).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("AMOUNT_MISMATCH"));
		topUp(player, 1000, "CASH").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_METHOD"));
		String paymentId = id(topUp(player, 1000).andExpect(status().isCreated())
			.andExpect(jsonPath("$.purpose").value("DEPOSIT"))
			.andExpect(jsonPath("$.status").value("PENDING")));
		topUp(player, 1000).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PAYMENT_PENDING"));
		send(player, get("/api/v1/me/deposit"), null).andExpect(jsonPath("$.balance").value(0))
			.andExpect(jsonPath("$.topUpBeingChecked").value(true))
			.andExpect(jsonPath("$.paymentInstructions").isNotEmpty());

		// Checked by FutSlot with the other payments made to it, never by the venue
		send(owner, get("/api/v1/owner/venues/" + venue.id() + "/payments/pending"), null)
			.andExpect(jsonPath("$.length()").value(0));
		TestUser admin = this.users.signUpAdmin("Deposit Admin");
		send(admin, get("/api/v1/admin/payments/pending"), null).andExpect(jsonPath("$[*].id", hasItem(paymentId)))
			.andExpect(jsonPath("$[?(@.id == '%s')].customerName".formatted(paymentId)).value(hasItem("Deposit Player")));
		send(admin, post("/api/v1/admin/payments/" + paymentId + "/approve"), null).andExpect(status().isOk());

		send(player, get("/api/v1/me/deposit"), null).andExpect(jsonPath("$.balance").value(1000.0))
			.andExpect(jsonPath("$.toAdd").value(0))
			.andExpect(jsonPath("$.history[0].type").value("TOP_UP"));
		send(player, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("DEPOSIT_TOPPED_UP"));
		book(player, venue, "07:00").andExpect(status().isCreated()).andExpect(jsonPath("$.depositBacked").value(true));
		topUp(player, 1000).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DEPOSIT_FULL"));
	}

	@Test
	void venuesThatDoNotRequireItIgnoreTheDeposit() throws Exception {
		TestUser owner = this.users.signUp("No Deposit Owner");
		TestVenue venue = policyVenue(owner, "No Deposit Futsal", 0, false);
		TestUser player = fullDeposit("Plain Player");
		String bookingId = id(book(player, venue, "07:00").andExpect(status().isCreated())
			.andExpect(jsonPath("$.depositBacked").value(false)));
		startsIn(bookingId, Duration.ofHours(5));

		send(player, post("/api/v1/bookings/" + bookingId + "/cancel"), "{}").andExpect(status().isOk())
			.andExpect(jsonPath("$.cancellationFeePercent").value(50));

		send(player, get("/api/v1/me/deposit"), null).andExpect(jsonPath("$.balance").value(1000.0));
	}

	@Test
	void aLateCancellationTakesTheFeeFromTheDeposit() throws Exception {
		TestUser owner = this.users.signUp("Late Deposit Owner");
		TestVenue venue = depositVenue(owner, "Late Deposit Futsal", 0);
		TestUser player = fullDeposit("Late Deposit Player");
		String bookingId = id(book(player, venue, "07:00"));
		String reference = JsonPath.read(send(player, get("/api/v1/bookings/" + bookingId), null).andReturn()
			.getResponse().getContentAsString(), "$.reference");
		startsIn(bookingId, Duration.ofHours(5));

		send(player, post("/api/v1/bookings/" + bookingId + "/cancel"), "{}").andExpect(status().isOk());

		// Half of the Rs 1200 price, with nothing paid up front
		send(player, get("/api/v1/me/deposit"), null).andExpect(jsonPath("$.balance").value(400.0))
			.andExpect(jsonPath("$.toAdd").value(600.0))
			.andExpect(jsonPath("$.history[0].type").value("FEE"))
			.andExpect(jsonPath("$.history[0].amount").value(-600.0))
			.andExpect(jsonPath("$.history[0].bookingReference").value(reference))
			.andExpect(jsonPath("$.history[0].venueName").value("Late Deposit Futsal"));
		send(player, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("DEPOSIT_FEE_TAKEN"))
			.andExpect(jsonPath("$.content[0].body").value(containsString("late (50% fee)")));
		send(owner, get("/api/v1/owner/venues/" + venue.id() + "/deposit-fees"), null)
			.andExpect(jsonPath("$.unpaid").value(600.0))
			.andExpect(jsonPath("$.fees[0].bookingReference").value(reference))
			.andExpect(jsonPath("$.fees[0].noShow").value(false))
			.andExpect(jsonPath("$.fees[0].feePercent").value(50));

		// Not full any more, so the venue won't take another booking until it's topped up
		book(player, venue, "09:00").andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(containsString("Add Rs 600")));
		topUp(player, 600).andExpect(status().isCreated());
	}

	@Test
	void theAdvanceCoversTheFeeFirst() throws Exception {
		TestUser owner = this.users.signUp("Advance Deposit Owner");
		TestVenue venue = depositVenue(owner, "Advance Deposit Futsal", 30);
		TestUser player = fullDeposit("Advance Deposit Player");
		String bookingId = id(book(player, venue, "07:00"));
		send(owner, post("/api/v1/owner/venues/" + venue.id() + "/bookings/" + bookingId + "/payments"),
				"{\"method\":\"CASH\",\"amount\":360}")
			.andExpect(status().isCreated());
		startsIn(bookingId, Duration.ofHours(5));

		send(player, post("/api/v1/bookings/" + bookingId + "/cancel"), "{}").andExpect(status().isOk());

		// The Rs 600 fee: the venue keeps the Rs 360 advance and the deposit pays the other Rs 240
		send(player, get("/api/v1/me/deposit"), null).andExpect(jsonPath("$.balance").value(760.0));
		send(owner, get("/api/v1/owner/venues/" + venue.id() + "/refunds"), null)
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void cancellingInTimeOrByTheVenueCostsTheDepositNothing() throws Exception {
		TestUser owner = this.users.signUp("Free Deposit Owner");
		TestVenue venue = depositVenue(owner, "Free Deposit Futsal", 0);
		TestUser player = fullDeposit("Free Deposit Player");
		String early = id(book(player, venue, "07:00"));
		send(player, post("/api/v1/bookings/" + early + "/cancel"), "{}").andExpect(status().isOk());
		String byVenue = id(book(player, venue, "08:00"));
		startsIn(byVenue, Duration.ofHours(2));
		send(owner, post("/api/v1/owner/venues/" + venue.id() + "/bookings/" + byVenue + "/cancel"), "{}")
			.andExpect(status().isOk());

		send(player, get("/api/v1/me/deposit"), null).andExpect(jsonPath("$.balance").value(1000.0));
	}

	@Test
	void aNoShowCostsTheLastMinuteFee() throws Exception {
		TestUser owner = this.users.signUp("No Show Owner");
		TestVenue venue = depositVenue(owner, "No Show Futsal", 0);
		TestUser player = fullDeposit("No Show Player");
		String bookingId = id(book(player, venue, "07:00"));
		startsIn(bookingId, Duration.ofMinutes(-30));

		send(owner, post("/api/v1/owner/venues/" + venue.id() + "/bookings/" + bookingId + "/no-show"), null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("NO_SHOW"))
			.andExpect(jsonPath("$.cancellationFeePercent").value(50));

		send(player, get("/api/v1/me/deposit"), null).andExpect(jsonPath("$.balance").value(400.0));
		send(player, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].body").value(containsString("not showing up")));
		send(owner, get("/api/v1/owner/venues/" + venue.id() + "/deposit-fees"), null)
			.andExpect(jsonPath("$.fees[0].noShow").value(true));
	}

	@Test
	void theDepositComesBackOnceNoBookingReliesOnIt() throws Exception {
		TestUser owner = this.users.signUp("Withdraw Owner");
		TestVenue venue = depositVenue(owner, "Withdraw Futsal", 0);
		TestUser player = fullDeposit("Withdraw Player");
		String bookingId = id(book(player, venue, "07:00"));
		String withdraw = "{\"method\":\"ESEWA\",\"sendTo\":\"9800000002\"}";

		send(player, get("/api/v1/me/deposit"), null).andExpect(jsonPath("$.inUse").value(true));
		send(player, post("/api/v1/me/deposit/withdrawals"), withdraw).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DEPOSIT_IN_USE"));
		send(player, post("/api/v1/bookings/" + bookingId + "/cancel"), "{}").andExpect(status().isOk());
		send(player, get("/api/v1/me/deposit"), null).andExpect(jsonPath("$.inUse").value(false));

		String withdrawalId = id(send(player, post("/api/v1/me/deposit/withdrawals"), withdraw)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.amount").value(1000.0))
			.andExpect(jsonPath("$.status").value("REQUESTED")));
		send(player, get("/api/v1/me/deposit"), null).andExpect(jsonPath("$.balance").value(0))
			.andExpect(jsonPath("$.withdrawal.id").value(withdrawalId));
		book(player, venue, "08:00").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DEPOSIT_REQUIRED"));
		topUp(player, 1000).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("WITHDRAWAL_PENDING"));
		send(player, post("/api/v1/me/deposit/withdrawals"), withdraw).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("WITHDRAWAL_PENDING"));

		// FutSlot sends it three days after the request, not before
		TestUser admin = this.users.signUpAdmin("Withdraw Admin");
		send(admin, get("/api/v1/admin/deposit-withdrawals"), null)
			.andExpect(jsonPath("$[?(@.id == '%s')].sendTo".formatted(withdrawalId)).value(hasItem("9800000002")))
			.andExpect(jsonPath("$[?(@.id == '%s')].playerName".formatted(withdrawalId)).value(hasItem("Withdraw Player")));
		String paid = "{\"reference\":\"ES-OUT-1\"}";
		send(admin, post("/api/v1/admin/deposit-withdrawals/" + withdrawalId + "/paid"), paid)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("WITHDRAWAL_NOT_DUE"));
		this.jdbc.update("UPDATE deposit_withdrawals SET due_at = now() - interval '1 minute' WHERE id = ?::uuid",
				withdrawalId);
		send(admin, post("/api/v1/admin/deposit-withdrawals/" + withdrawalId + "/paid"), paid)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PAID"))
			.andExpect(jsonPath("$.reference").value("ES-OUT-1"));
		send(player, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("DEPOSIT_RETURNED"))
			.andExpect(jsonPath("$.content[0].body").value(containsString("9800000002")));
		send(player, get("/api/v1/me/deposit"), null).andExpect(jsonPath("$.withdrawal").isEmpty());
	}

	@Test
	void aWithdrawalCanBeCalledOffBeforeItIsSent() throws Exception {
		TestUser player = fullDeposit("Changed Mind Player");
		String withdrawalId = id(send(player, post("/api/v1/me/deposit/withdrawals"),
				"{\"method\":\"KHALTI\",\"sendTo\":\"9800000003\"}").andExpect(status().isCreated()));

		send(player, post("/api/v1/me/deposit/withdrawals/" + withdrawalId + "/cancel"), null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"));

		send(player, get("/api/v1/me/deposit"), null).andExpect(jsonPath("$.balance").value(1000.0))
			.andExpect(jsonPath("$.history[0].type").value("WITHDRAWAL_CANCELLED"));
		send(this.users.signUp("Someone Else"), post("/api/v1/me/deposit/withdrawals/" + withdrawalId + "/cancel"), null)
			.andExpect(status().isNotFound());
	}

	@Test
	void feesArePaidToTheVenueAfterTheMonthEnds() throws Exception {
		TestUser owner = this.users.signUp("Payout Owner");
		TestVenue venue = depositVenue(owner, "Payout Futsal", 0);
		TestUser player = fullDeposit("Payout Player");
		String bookingId = id(book(player, venue, "07:00"));
		startsIn(bookingId, Duration.ofHours(5));
		send(player, post("/api/v1/bookings/" + bookingId + "/cancel"), "{}").andExpect(status().isOk());

		// Still this month's: nothing to pay yet
		this.payouts.closeEndedMonths();
		send(owner, get("/api/v1/owner/venues/" + venue.id() + "/deposit-fees"), null)
			.andExpect(jsonPath("$.unpaid").value(600.0))
			.andExpect(jsonPath("$.payouts.length()").value(0));

		this.jdbc.update("UPDATE deposit_entries SET created_at = created_at - interval '40 days' WHERE booking_id = ?::uuid",
				bookingId);
		this.payouts.closeEndedMonths();
		send(owner, get("/api/v1/owner/venues/" + venue.id() + "/deposit-fees"), null)
			.andExpect(jsonPath("$.unpaid").value(0))
			.andExpect(jsonPath("$.payouts[0].amount").value(600.0))
			.andExpect(jsonPath("$.payouts[0].status").value("PENDING"));
		TestUser admin = this.users.signUpAdmin("Payout Admin");
		String payouts = send(admin, get("/api/v1/admin/payouts"), null).andExpect(status().isOk()).andReturn()
			.getResponse().getContentAsString();
		String payoutId = JsonPath.<java.util.List<String>>read(payouts,
				"$[?(@.venueId == '%s')].id".formatted(venue.id())).getFirst();

		// Running it again doesn't pay the same fees twice
		this.payouts.closeEndedMonths();
		send(owner, get("/api/v1/owner/venues/" + venue.id() + "/deposit-fees"), null)
			.andExpect(jsonPath("$.payouts.length()").value(1));

		send(admin, post("/api/v1/admin/payouts/" + payoutId + "/paid"), "{\"method\":\"BANK_TRANSFER\",\"reference\":\"NIC-77\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PAID"));
		send(owner, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("DEPOSIT_PAYOUT_SENT"))
			.andExpect(jsonPath("$.content[0].body").value(containsString("Rs 600")));
		send(this.users.signUp("Not Staff"), get("/api/v1/owner/venues/" + venue.id() + "/deposit-fees"), null)
			.andExpect(status().isNotFound());
	}

	@Test
	void onlyAdminsSendDepositMoney() throws Exception {
		TestUser player = this.users.signUp("Curious Player");
		send(player, get("/api/v1/admin/deposit-withdrawals"), null).andExpect(status().isForbidden());
		send(player, get("/api/v1/admin/payouts"), null).andExpect(status().isForbidden());
	}

	/** A new player whose deposit FutSlot has checked and filled. */
	private TestUser fullDeposit(String name) throws Exception {
		TestUser player = this.users.signUp(name);
		String paymentId = id(topUp(player, 1000).andExpect(status().isCreated()));
		send(this.users.signUpAdmin("Top-up Admin"), post("/api/v1/admin/payments/" + paymentId + "/approve"), null)
			.andExpect(status().isOk());
		return player;
	}

	private TestVenue depositVenue(TestUser owner, String name, int advancePercent) throws Exception {
		return policyVenue(owner, name, advancePercent, true);
	}

	private TestVenue policyVenue(TestUser owner, String name, int advancePercent, boolean depositRequired)
			throws Exception {
		TestVenue venue = this.venues.active(owner, name);
		send(owner, put("/api/v1/owner/venues/" + venue.id() + "/policy"), """
				{"advancePercent":%d,"bookingHoldMinutes":15,"cancellationTiers":%s,"maxAdvanceBookingDays":30,"depositRequired":%s}"""
			.formatted(advancePercent, RISING_FEES, depositRequired))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.policy.depositRequired").value(depositRequired));
		return venue;
	}

	private ResultActions book(TestUser player, TestVenue venue, String time) throws Exception {
		return send(player, post("/api/v1/bookings"),
				"{\"courtId\":\"%s\",\"startsAt\":\"%sT%s:00+05:45\"}".formatted(venue.courtId(), GAME_DAY, time));
	}

	private ResultActions topUp(TestUser player, int amount) throws Exception {
		return topUp(player, amount, "ESEWA");
	}

	private ResultActions topUp(TestUser player, int amount, String method) throws Exception {
		return send(player, post("/api/v1/me/deposit/top-ups"), "{\"method\":\"%s\",\"transactionId\":\"DEP-%s\",\"amount\":%d}"
			.formatted(method, UUID.randomUUID(), amount));
	}

	/** Moves a booking to start {@code notice} from now (negative: already started), keeping its length. */
	private void startsIn(String bookingId, Duration notice) {
		this.jdbc.update("""
				UPDATE bookings SET ends_at = now() + ?::interval + (ends_at - starts_at), starts_at = now() + ?::interval
				WHERE id = ?::uuid""", notice.toString(), notice.toString(), bookingId);
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
