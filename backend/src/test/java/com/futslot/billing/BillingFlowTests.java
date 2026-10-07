package com.futslot.billing;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
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
class BillingFlowTests {

	private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Kathmandu"));

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	SubscriptionService subscriptions;

	@Autowired
	FeaturedService featured;

	@Test
	void ownerSubscribesPaysFutSlotAndAnAdminVerifies() throws Exception {
		TestUser admin = this.users.signUpAdmin("Billing Admin");
		String code = plan(admin, 1500, 15000);
		TestUser owner = this.users.signUp("Paying Owner");
		TestVenue venue = this.venues.active(owner, "Paying Futsal");
		String subscription = "/api/v1/owner/venues/" + venue.id() + "/subscription";

		this.mvc.perform(get("/api/v1/subscription-plans"))
			.andExpect(jsonPath("$[*].code", hasItem("FREE")))
			.andExpect(jsonPath("$[*].code", hasItem(code)));
		send(owner, get(subscription), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.plan.code").value("FREE"))
			.andExpect(jsonPath("$.status").doesNotExist())
			.andExpect(jsonPath("$.limitsEnforced").value(false))
			.andExpect(jsonPath("$.courts").value(1));
		String managerPhone = TestUsers.nextLocalPhone();
		send(owner, post("/api/v1/owner/venues/" + venue.id() + "/staff"),
				"{\"phone\":\"%s\",\"fullName\":\"Manager\",\"role\":\"MANAGER\"}".formatted(managerPhone))
			.andExpect(status().isCreated());
		send(this.users.logIn(managerPhone, "Manager"), get(subscription), null).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("OWNER_ONLY"));

		String invoiceId = JsonPath.read(send(owner, put(subscription), "{\"planCode\":\"%s\"}".formatted(code))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PAST_DUE"))
			.andExpect(jsonPath("$.plan.code").value(code))
			.andExpect(jsonPath("$.openInvoice.amount").value(1500.0))
			.andExpect(jsonPath("$.openInvoice.paymentPending").value(false))
			.andExpect(jsonPath("$.openInvoice.invoiceNumber", startsWith("INV-")))
			.andExpect(jsonPath("$.paymentInstructions").value("Contact FutSlot for payment details"))
			.andReturn().getResponse().getContentAsString(), "$.openInvoice.id");
		send(owner, get("/api/v1/notifications"), null).andExpect(jsonPath("$.content[0].type").value("INVOICE_DUE"));

		send(owner, post("/api/v1/invoices/" + invoiceId + "/payments"),
				"{\"method\":\"ESEWA\",\"transactionId\":\"ES-%s\",\"amount\":1000}".formatted(UUID.randomUUID()))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("AMOUNT_MISMATCH"));
		String paymentId = JsonPath.read(send(owner, post("/api/v1/invoices/" + invoiceId + "/payments"),
				"{\"method\":\"ESEWA\",\"transactionId\":\"ES-%s\",\"amount\":1500}".formatted(UUID.randomUUID()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.purpose").value("SUBSCRIPTION"))
			.andExpect(jsonPath("$.customerName").value("Paying Futsal"))
			.andReturn().getResponse().getContentAsString(), "$.id");
		send(owner, get(subscription), null).andExpect(jsonPath("$.openInvoice.paymentPending").value(true));
		send(owner, get("/api/v1/owner/venues/" + venue.id() + "/invoices"), null)
			.andExpect(jsonPath("$[0].paymentPending").value(true));

		// Paid to FutSlot, not to the venue: not in the venue's queue, its income or its own approvals
		send(owner, get("/api/v1/owner/venues/" + venue.id() + "/payments/pending"), null)
			.andExpect(jsonPath("$[*].id", not(hasItem(paymentId))));
		send(owner, post("/api/v1/owner/venues/" + venue.id() + "/payments/" + paymentId + "/approve"), null)
			.andExpect(status().isNotFound());
		send(owner, post("/api/v1/admin/payments/" + paymentId + "/approve"), null).andExpect(status().isForbidden());
		send(admin, get("/api/v1/admin/payments/pending"), null).andExpect(jsonPath("$[*].id", hasItem(paymentId)));
		send(admin, post("/api/v1/admin/payments/" + paymentId + "/approve"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("SUCCEEDED"));

		send(owner, get(subscription), null)
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.openInvoice").doesNotExist())
			.andExpect(jsonPath("$.currentPeriodEnd", startsWith(TODAY.plusMonths(1).toString().substring(0, 7))));
		send(owner, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("SUBSCRIPTION_ACTIVE"));
		send(owner, get("/api/v1/owner/venues/" + venue.id() + "/reports/summary"), null)
			.andExpect(jsonPath("$.money.collected").value(0));
		send(owner, get("/api/v1/owner/venues/" + venue.id() + "/invoices"), null)
			.andExpect(jsonPath("$[0].status").value("PAID"));
	}

	@Test
	void renewalsAreInvoicedAheadPlanChangesWaitAndUnpaidRenewalsGoPastDue() throws Exception {
		TestUser admin = this.users.signUpAdmin("Renewal Admin");
		String code = plan(admin, 1000, 10000);
		TestUser owner = this.users.signUp("Renewing Owner");
		TestVenue venue = this.venues.active(owner, "Renewing Futsal");
		String subscription = "/api/v1/owner/venues/" + venue.id() + "/subscription";
		String firstInvoice = JsonPath.read(send(owner, put(subscription), "{\"planCode\":\"%s\"}".formatted(code))
			.andReturn().getResponse().getContentAsString(), "$.openInvoice.id");
		send(admin, post("/api/v1/admin/invoices/" + firstInvoice + "/payments"), "{\"method\":\"BANK_TRANSFER\"}")
			.andExpect(status().isCreated());

		// Two days before the period ends a renewal invoice goes out
		periodEndsIn(venue, "2 days");
		this.subscriptions.runBilling();
		send(owner, get(subscription), null).andExpect(jsonPath("$.openInvoice.amount").value(1000.0));

		// Switching to yearly replaces the renewal invoice; the current period stays monthly
		send(owner, put(subscription), "{\"planCode\":\"%s\",\"billingCycle\":\"YEARLY\"}".formatted(code))
			.andExpect(jsonPath("$.billingCycle").value("MONTHLY"))
			.andExpect(jsonPath("$.nextBillingCycle").value("YEARLY"))
			.andExpect(jsonPath("$.openInvoice.amount").value(10000.0));

		periodEndsIn(venue, "-1 hour");
		this.subscriptions.runBilling();
		String renewal = JsonPath.read(send(owner, get(subscription), null)
			.andExpect(jsonPath("$.status").value("PAST_DUE"))
			.andExpect(jsonPath("$.plan.code").value(code))
			.andReturn().getResponse().getContentAsString(), "$.openInvoice.id");
		send(owner, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("SUBSCRIPTION_PAST_DUE"));

		send(admin, post("/api/v1/admin/invoices/" + renewal + "/payments"), "{\"method\":\"CASH\"}")
			.andExpect(status().isCreated());
		send(owner, get(subscription), null)
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.billingCycle").value("YEARLY"))
			.andExpect(jsonPath("$.nextPlan").doesNotExist());
	}

	@Test
	void goingBackToFreeWaitsForThePeriodAndUnpaidSubscriptionsLapse() throws Exception {
		TestUser admin = this.users.signUpAdmin("Lapse Admin");
		String code = plan(admin, 800, null);
		TestUser owner = this.users.signUp("Leaving Owner");
		TestVenue venue = this.venues.active(owner, "Leaving Futsal");
		String subscription = "/api/v1/owner/venues/" + venue.id() + "/subscription";
		send(owner, put(subscription), "{\"planCode\":\"%s\",\"billingCycle\":\"YEARLY\"}".formatted(code))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CYCLE_NOT_OFFERED"));
		String invoice = JsonPath.read(send(owner, put(subscription), "{\"planCode\":\"%s\"}".formatted(code))
			.andReturn().getResponse().getContentAsString(), "$.openInvoice.id");
		send(admin, post("/api/v1/admin/invoices/" + invoice + "/payments"), "{\"method\":\"CASH\"}");

		send(owner, put(subscription), "{\"planCode\":\"FREE\"}").andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.cancelAtPeriodEnd").value(true));
		periodEndsIn(venue, "-1 hour");
		this.subscriptions.runBilling();
		send(owner, get(subscription), null).andExpect(jsonPath("$.plan.code").value("FREE"))
			.andExpect(jsonPath("$.status").doesNotExist());
		send(owner, get("/api/v1/notifications"), null).andExpect(jsonPath("$.content[0].type").value("SUBSCRIPTION_ENDED"));

		// Choosing a plan and never paying lapses after the grace period
		send(owner, put(subscription), "{\"planCode\":\"%s\"}".formatted(code)).andExpect(jsonPath("$.status").value("PAST_DUE"));
		this.jdbc.update("""
				UPDATE venue_subscriptions SET created_at = now() - interval '8 days'
				WHERE venue_id = ?::uuid AND status = 'PAST_DUE'""", venue.id().toString());
		this.subscriptions.runBilling();
		send(owner, get(subscription), null).andExpect(jsonPath("$.plan.code").value("FREE"));
		send(owner, get("/api/v1/owner/venues/" + venue.id() + "/invoices"), null)
			.andExpect(jsonPath("$[0].status").value("VOID"));
	}

	@Test
	void venuesBuyFeaturedDaysWhilePlacesLast() throws Exception {
		TestUser admin = this.users.signUpAdmin("Featured Admin");
		LocalDate from = TODAY.plusDays(90);
		TestUser owner = this.users.signUp("Featured Owner");
		TestVenue venue = this.venues.active(owner, "Featured Futsal");
		String featuredPath = "/api/v1/owner/venues/" + venue.id() + "/featured";

		send(owner, get(featuredPath + "/quote").param("placement", "SEARCH_TOP").param("startsOn", from.toString())
			.param("days", "3"), null)
			.andExpect(jsonPath("$.price").value(600))
			.andExpect(jsonPath("$.available").value(true));
		String listingId = JsonPath.read(send(owner, post(featuredPath), feature(from))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
			.andExpect(jsonPath("$.price").value(600.0))
			.andReturn().getResponse().getContentAsString(), "$.id");
		send(owner, post(featuredPath), feature(from.plusDays(1))).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_FEATURED"));

		String paymentId = JsonPath.read(send(owner, post("/api/v1/featured-listings/" + listingId + "/payments"),
				"{\"method\":\"KHALTI\",\"transactionId\":\"KH-%s\",\"amount\":600}".formatted(UUID.randomUUID()))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
		send(admin, post("/api/v1/admin/payments/" + paymentId + "/approve"), null).andExpect(status().isOk());
		send(owner, get(featuredPath), null).andExpect(jsonPath("$[0].status").value("SCHEDULED"));
		send(owner, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("FEATURED_CONFIRMED"));

		// Three places on SEARCH_TOP: two more venues take them, the fourth is turned away
		for (int i = 1; i <= 2; i++) {
			TestUser other = this.users.signUp("Rival Owner " + i);
			TestVenue rival = this.venues.active(other, "Rival Futsal " + i);
			send(other, post("/api/v1/owner/venues/" + rival.id() + "/featured"), feature(from)).andExpect(status().isCreated());
		}
		TestUser late = this.users.signUp("Late Owner");
		TestVenue lateVenue = this.venues.active(late, "Late Futsal");
		send(late, post("/api/v1/owner/venues/" + lateVenue.id() + "/featured"), feature(from))
			.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PLACEMENT_FULL"));
		send(late, post("/api/v1/owner/venues/" + lateVenue.id() + "/featured"), feature(from.plusDays(10)))
			.andExpect(status().isCreated());

		// Once its days come, the venue shows in the featured strip
		this.mvc.perform(get("/api/v1/featured-venues").param("placement", "SEARCH_TOP"))
			.andExpect(jsonPath("$[*].name", not(hasItem("Featured Futsal"))));
		this.jdbc.update("UPDATE featured_listings SET starts_at = now() - interval '1 hour' WHERE id = ?::uuid", listingId);
		this.featured.maintain();
		this.mvc.perform(get("/api/v1/featured-venues").param("placement", "SEARCH_TOP").param("city", "Kathmandu"))
			.andExpect(jsonPath("$[*].name", hasItem("Featured Futsal")));
		send(owner, get(featuredPath), null).andExpect(jsonPath("$[0].status").value("ACTIVE"));
	}

	private String plan(TestUser admin, int monthly, Integer yearly) throws Exception {
		String code = "PRO_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		send(admin, post("/api/v1/admin/subscription-plans"), """
				{"code":"%s","name":"Pro","monthlyPrice":%d%s,"maxCourts":5,"maxStaff":5,"features":["REPORTS"]}"""
			.formatted(code, monthly, (yearly != null) ? ",\"yearlyPrice\":" + yearly : "")).andExpect(status().isCreated());
		return code;
	}

	private void periodEndsIn(TestVenue venue, String interval) {
		this.jdbc.update("""
				UPDATE venue_subscriptions SET current_period_start = now() + ?::interval - interval '1 month',
				    current_period_end = now() + ?::interval
				WHERE venue_id = ?::uuid AND status IN ('ACTIVE', 'PAST_DUE')""", interval, interval,
				venue.id().toString());
		this.jdbc.update("""
				UPDATE subscription_invoices i SET period_start = s.current_period_end,
				    period_end = s.current_period_end + interval '1 month'
				FROM venue_subscriptions s
				WHERE i.subscription_id = s.id AND s.venue_id = ?::uuid AND i.status IN ('DUE', 'OVERDUE')""",
				venue.id().toString());
	}

	private static String feature(LocalDate from) {
		return "{\"placement\":\"SEARCH_TOP\",\"startsOn\":\"%s\",\"days\":3}".formatted(from);
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request, String json) throws Exception {
		request.header("Authorization", user.bearer());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request);
	}

}
