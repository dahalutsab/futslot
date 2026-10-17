package com.futslot.payment;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
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
class PaymentFlowTests {

	private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Kathmandu"));

	/** Far enough ahead that a 24h free-cancellation window is always still open. */
	private static final LocalDate GAME_DAY = TODAY.plusDays(3);

	private static final String FULL_FEE_INSIDE_A_DAY = "[{\"withinHours\":24,\"feePercent\":100}]";

	private static final String RISING_FEES = "[{\"withinHours\":24,\"feePercent\":20},{\"withinHours\":12,\"feePercent\":50}]";

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void verifiedWalletPaymentConfirmsTheBookingAndKeepsTheSlotMeanwhile() throws Exception {
		TestUser owner = this.users.signUp("Pay Owner");
		TestVenue venue = advanceVenue(owner, "Pay Futsal", 30, FULL_FEE_INSIDE_A_DAY);
		TestUser player = this.users.signUp("Pay Player");
		String bookingId = id(book(player, venue, "07:00"));

		send(player, get("/api/v1/bookings/" + bookingId + "/payments"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.advanceRequired").value(360.0))
			.andExpect(jsonPath("$.amountDue").value(1200.0))
			.andExpect(jsonPath("$.paymentInstructions").value("eSewa/Khalti 9800000001"));
		String paymentId = id(claim(player, bookingId, "ESEWA", "ES-" + UUID.randomUUID(), 360)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("PENDING")));

		// Still held while staff check: shown as booked, and nobody else can take it
		this.mvc.perform(get("/api/v1/venues/" + venue.slug() + "/availability").param("date", GAME_DAY.toString()))
			.andExpect(jsonPath("$.courts[0].slots[1].status").value("BOOKED"));
		book(this.users.signUp("Impatient"), venue, "07:00").andExpect(status().isConflict());
		send(owner, get("/api/v1/notifications"), null).andExpect(jsonPath("$.content[0].type").value("PAYMENT_TO_VERIFY"));
		send(owner, get(venuePath(venue) + "/payments/pending"), null).andExpect(jsonPath("$[*].id", hasItem(paymentId)));

		send(owner, post(venuePath(venue) + "/payments/" + paymentId + "/approve"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("SUCCEEDED"));
		send(player, get("/api/v1/bookings/" + bookingId), null).andExpect(jsonPath("$.status").value("CONFIRMED"))
			.andExpect(jsonPath("$.paymentStatus").value("PARTIALLY_PAID"))
			.andExpect(jsonPath("$.paidAmount").value(360.0))
			.andExpect(jsonPath("$.holdExpiresAt").value(nullValue()));
		send(player, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("PAYMENT_RECEIVED"))
			.andExpect(jsonPath("$.content[0].body").value(containsString("confirmed")));
	}

	@Test
	void rejectedPaymentGivesThePlayerTimeToPayAgain() throws Exception {
		TestUser owner = this.users.signUp("Reject Owner");
		TestVenue venue = advanceVenue(owner, "Reject Futsal", 30, FULL_FEE_INSIDE_A_DAY);
		TestUser player = this.users.signUp("Reject Player");
		String bookingId = id(book(player, venue, "08:00"));
		String paymentId = id(claim(player, bookingId, "KHALTI", "KH-" + UUID.randomUUID(), 360));

		send(owner, post(venuePath(venue) + "/payments/" + paymentId + "/reject"), "{\"reason\":\"Not in our Khalti\"}")
			.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FAILED"));

		send(player, get("/api/v1/bookings/" + bookingId), null).andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
			.andExpect(jsonPath("$.holdExpiresAt").value(notNullValue()));
		send(player, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("PAYMENT_REJECTED"))
			.andExpect(jsonPath("$.content[0].body").value(containsString("Not in our Khalti")));
		claim(player, bookingId, "KHALTI", "KH-" + UUID.randomUUID(), 360).andExpect(status().isCreated());
	}

	@Test
	void claimsAreValidated() throws Exception {
		TestUser owner = this.users.signUp("Rules Pay Owner");
		TestVenue venue = advanceVenue(owner, "Rules Pay Futsal", 30, FULL_FEE_INSIDE_A_DAY);
		TestUser player = this.users.signUp("Rules Pay Player");
		String bookingId = id(book(player, venue, "09:00"));
		String otherBooking = id(book(this.users.signUp("Other Payer"), venue, "10:00"));
		String transaction = "ES-" + UUID.randomUUID();

		claim(player, bookingId, "ESEWA", "ES-x", 100).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("AMOUNT_TOO_LOW"));
		claim(player, bookingId, "ESEWA", "ES-x", 5000).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("AMOUNT_TOO_HIGH"));
		claim(player, bookingId, "CASH", "cash", 360).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_METHOD"));
		claim(player, otherBooking, "ESEWA", "ES-x", 360).andExpect(status().isNotFound());

		claim(player, bookingId, "ESEWA", transaction, 360).andExpect(status().isCreated());
		claim(player, bookingId, "ESEWA", "ES-" + UUID.randomUUID(), 360).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PAYMENT_PENDING"));
		TestUser other = this.users.signUp("Copycat");
		String copyBooking = id(book(other, venue, "11:00"));
		claim(other, copyBooking, "ESEWA", transaction, 360).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DUPLICATE_TRANSACTION"));
	}

	@Test
	void staffRecordCashAgainstWhatIsDue() throws Exception {
		TestUser owner = this.users.signUp("Cash Owner");
		TestVenue venue = this.venues.active(owner, "Cash Futsal");
		TestUser player = this.users.signUp("Cash Player");
		String bookingId = id(book(player, venue, "07:00"));

		claim(player, bookingId, "ESEWA", "ES-" + UUID.randomUUID(), 1200).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("NO_PAYMENT_DUE"));
		record(owner, venue, bookingId, "CASH", 2000).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("AMOUNT_TOO_HIGH"));
		record(owner, venue, bookingId, "CASH", 1200).andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("SUCCEEDED"));

		send(owner, get(venuePath(venue) + "/bookings/" + bookingId + "/payments"), null)
			.andExpect(jsonPath("$.paymentStatus").value("PAID"))
			.andExpect(jsonPath("$.amountDue").value(0.0));
		send(player, get("/api/v1/notifications"), null).andExpect(jsonPath("$.content[0].type").value("PAYMENT_RECEIVED"));
	}

	@Test
	void playerCancellingInTimeIsOwedEverythingAndTheVenueMarksItRefunded() throws Exception {
		TestUser owner = this.users.signUp("Refund Owner");
		TestVenue venue = this.venues.active(owner, "Refund Futsal");
		TestUser player = this.users.signUp("Refund Player");
		String bookingId = id(book(player, venue, "07:00"));
		record(owner, venue, bookingId, "ESEWA", 1200).andExpect(status().isCreated());

		send(player, post("/api/v1/bookings/" + bookingId + "/cancel"), "{}").andExpect(status().isOk());

		String refundId = JsonPath.read(send(owner, get(venuePath(venue) + "/refunds"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[0].amount").value(1200.0))
			.andExpect(jsonPath("$[0].originalMethod").value("ESEWA"))
			.andReturn().getResponse().getContentAsString(), "$[0].id");
		send(player, get("/api/v1/notifications"), null).andExpect(jsonPath("$.content[*].type", hasItem("REFUND_DUE")));

		send(owner, post(venuePath(venue) + "/refunds/" + refundId + "/complete"), "{\"reference\":\"ES-REF-1\"}")
			.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCEEDED"));
		send(owner, post(venuePath(venue) + "/refunds/" + refundId + "/complete"), null).andExpect(status().isConflict());
		send(player, get("/api/v1/bookings/" + bookingId + "/payments"), null)
			.andExpect(jsonPath("$.paymentStatus").value("REFUNDED"))
			.andExpect(jsonPath("$.payments[0].status").value("REFUNDED"))
			.andExpect(jsonPath("$.refunds[0].reference").value("ES-REF-1"));
		send(player, get("/api/v1/notifications"), null).andExpect(jsonPath("$.content[0].type").value("REFUND_SENT"));
	}

	@Test
	void lateCancellationKeepsTheTierFeeOfTheBookingPrice() throws Exception {
		TestUser owner = this.users.signUp("Late Owner");
		TestVenue venue = advanceVenue(owner, "Late Futsal", 0, RISING_FEES);
		TestUser player = this.users.signUp("Late Player");
		String booked = book(player, venue, "07:00").andReturn().getResponse().getContentAsString();
		String bookingId = JsonPath.read(booked, "$.id");
		record(owner, venue, bookingId, "CASH", 1200);
		startsIn(bookingId, Duration.ofHours(5));

		send(player, post("/api/v1/bookings/" + bookingId + "/cancel"), "{}").andExpect(status().isOk())
			.andExpect(jsonPath("$.cancellationFeePercent").value(50));

		send(owner, get(venuePath(venue) + "/refunds"), null).andExpect(jsonPath("$[0].amount").value(600.0))
			.andExpect(jsonPath("$[0].reason").value(containsString("50% fee")));
		String customerId = JsonPath.read(booked, "$.customer.id");
		send(owner, get(venuePath(venue) + "/customers/" + customerId), null)
			.andExpect(jsonPath("$.stats.lateCancellations").value(1));
	}

	@Test
	void lateCancellationFeeIsCappedAtWhatWasPaid() throws Exception {
		TestUser owner = this.users.signUp("Capped Owner");
		TestVenue venue = advanceVenue(owner, "Capped Futsal", 0, RISING_FEES);
		TestUser player = this.users.signUp("Capped Player");
		String bookingId = id(book(player, venue, "07:00"));
		record(owner, venue, bookingId, "CASH", 400);
		startsIn(bookingId, Duration.ofHours(5));

		// Half of 1200 is more than the 400 paid, so the venue keeps the 400
		send(player, post("/api/v1/bookings/" + bookingId + "/cancel"), "{}").andExpect(status().isOk());

		send(owner, get(venuePath(venue) + "/refunds"), null).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void cancellingADayAheadIsFree() throws Exception {
		TestUser owner = this.users.signUp("Early Owner");
		TestVenue venue = advanceVenue(owner, "Early Futsal", 0, RISING_FEES);
		TestUser player = this.users.signUp("Early Player");
		String bookingId = id(book(player, venue, "07:00"));
		record(owner, venue, bookingId, "CASH", 1200);

		send(player, post("/api/v1/bookings/" + bookingId + "/cancel"), "{}").andExpect(status().isOk())
			.andExpect(jsonPath("$.cancellationFeePercent").value(0));

		send(owner, get(venuePath(venue) + "/refunds"), null).andExpect(jsonPath("$[0].amount").value(1200.0))
			.andExpect(jsonPath("$[0].reason").value("Cancelled by player"));
	}

	@Test
	void playersAtTheStrikeLimitPayMoreUpFront() throws Exception {
		TestUser owner = this.users.signUp("Strike Owner");
		TestVenue venue = this.venues.active(owner, "Strike Futsal");
		send(owner, put(venuePath(venue) + "/policy"), """
				{"advancePercent":0,"bookingHoldMinutes":15,"cancellationTiers":%s,"maxAdvanceBookingDays":30,"strikeLimit":1,"strikeAdvancePercent":50}"""
			.formatted(RISING_FEES)).andExpect(status().isOk());
		TestUser player = this.users.signUp("Strike Player");
		String quote = "{\"courtId\":\"%s\",\"startsAt\":\"%sT08:00:00+05:45\"}".formatted(venue.courtId(), GAME_DAY);

		send(player, post("/api/v1/bookings/quote"), quote).andExpect(jsonPath("$.advanceRequired").value(0))
			.andExpect(jsonPath("$.strikeAdvance").value(false));
		String booked = book(player, venue, "07:00").andReturn().getResponse().getContentAsString();
		String bookingId = JsonPath.read(booked, "$.id");
		startsIn(bookingId, Duration.ofHours(5));
		send(player, post("/api/v1/bookings/" + bookingId + "/cancel"), "{}").andExpect(status().isOk());

		// One late cancellation reaches the limit: half the price up front from now on
		send(player, post("/api/v1/bookings/quote"), quote).andExpect(jsonPath("$.advanceRequired").value(600.0))
			.andExpect(jsonPath("$.strikeAdvance").value(true));
		book(player, venue, "08:00").andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
			.andExpect(jsonPath("$.advanceRequired").value(600.0));
		String customerId = JsonPath.read(booked, "$.customer.id");
		send(owner, get(venuePath(venue) + "/customers/" + customerId), null)
			.andExpect(jsonPath("$.stats.recentStrikes").value(1))
			.andExpect(jsonPath("$.strikeAdvancePercent").value(50));

		// Another player at the same venue is unaffected
		book(this.users.signUp("Clean Player"), venue, "09:00").andExpect(jsonPath("$.status").value("CONFIRMED"));
	}

	@Test
	void venueCancellationAndExpiredHoldsRefundInFull() throws Exception {
		TestUser owner = this.users.signUp("Full Refund Owner");
		TestVenue venue = advanceVenue(owner, "Full Refund Futsal", 50, FULL_FEE_INSIDE_A_DAY);
		TestUser player = this.users.signUp("Full Refund Player");
		String confirmed = id(book(player, venue, "07:00"));
		record(owner, venue, confirmed, "CASH", 600);
		send(owner, post(venuePath(venue) + "/bookings/" + confirmed + "/cancel"), "{}").andExpect(status().isOk());

		// Paid something but not the advance, then the hold ran out
		String partial = id(book(player, venue, "08:00"));
		record(owner, venue, partial, "CASH", 100);
		this.jdbc.update("UPDATE bookings SET hold_expires_at = now() - interval '1 second' WHERE id = ?::uuid", partial);
		book(this.users.signUp("Next In Line"), venue, "08:00").andExpect(status().isCreated());

		send(owner, get(venuePath(venue) + "/refunds"), null).andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].amount").value(600.0))
			.andExpect(jsonPath("$[1].amount").value(100.0))
			.andExpect(jsonPath("$[1].reason").value(containsString("expired")));
	}

	private TestVenue advanceVenue(TestUser owner, String name, int advancePercent, String cancellationTiers)
			throws Exception {
		TestVenue venue = this.venues.active(owner, name);
		send(owner, put(venuePath(venue) + "/policy"), """
				{"advancePercent":%d,"bookingHoldMinutes":15,"cancellationTiers":%s,"maxAdvanceBookingDays":30}"""
			.formatted(advancePercent, cancellationTiers)).andExpect(status().isOk());
		send(owner, put(venuePath(venue) + "/payment-instructions"), "{\"instructions\":\"eSewa/Khalti 9800000001\"}")
			.andExpect(status().isOk());
		return venue;
	}

	private ResultActions book(TestUser player, TestVenue venue, String time) throws Exception {
		return send(player, post("/api/v1/bookings"),
				"{\"courtId\":\"%s\",\"startsAt\":\"%sT%s:00+05:45\"}".formatted(venue.courtId(), GAME_DAY, time));
	}

	private ResultActions claim(TestUser player, String bookingId, String method, String transactionId, int amount)
			throws Exception {
		return send(player, post("/api/v1/bookings/" + bookingId + "/payments"),
				"{\"method\":\"%s\",\"transactionId\":\"%s\",\"amount\":%d}".formatted(method, transactionId, amount));
	}

	private ResultActions record(TestUser staff, TestVenue venue, String bookingId, String method, int amount)
			throws Exception {
		return send(staff, post(venuePath(venue) + "/bookings/" + bookingId + "/payments"),
				"{\"method\":\"%s\",\"amount\":%d}".formatted(method, amount));
	}

	private static String venuePath(TestVenue venue) {
		return "/api/v1/owner/venues/" + venue.id();
	}

	/** Moves a booking to start {@code notice} from now, keeping its length. */
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
