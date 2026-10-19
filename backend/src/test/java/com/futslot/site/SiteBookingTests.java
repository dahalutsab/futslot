package com.futslot.site;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import com.futslot.support.IntegrationTest;
import com.futslot.support.RecordingSmsSender;
import com.futslot.support.TestUsers;
import com.futslot.support.TestUsers.TestUser;
import com.futslot.support.TestVenues;
import com.futslot.support.TestVenues.TestVenue;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

@IntegrationTest
class SiteBookingTests {

	private static final LocalDate TOMORROW = LocalDate.now(ZoneId.of("Asia/Kathmandu")).plusDays(1);

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Autowired
	RecordingSmsSender sms;

	@Autowired
	SiteSession siteSession;

	@Test
	void playersSignInAndBookOnAVenuesSite() throws Exception {
		TestUser owner = this.users.signUp("Booking Site Owner");
		TestVenue venue = this.venues.active(owner, "Book Here Futsal");
		ownerSends(owner, put("/api/v1/owner/venues/" + venue.id() + "/policy"),
				"{\"advancePercent\":30,\"bookingHoldMinutes\":15,\"cancellationTiers\":[{\"withinHours\":24,\"feePercent\":100}],\"maxAdvanceBookingDays\":30}");
		ownerSends(owner, put("/api/v1/owner/venues/" + venue.id() + "/site"), "{\"template\":\"TURF\",\"subdomain\":\"bookhere\"}");
		ownerSends(owner, post("/api/v1/owner/venues/" + venue.id() + "/site/publish"), null);

		// Free slots on the site link to booking
		String availability = this.mvc.perform(get("/api/v1/venues/" + venue.slug() + "/availability").param("date", TOMORROW.toString()))
			.andReturn().getResponse().getContentAsString();
		List<Map<String, Object>> free = JsonPath.read(availability, "$.courts[0].slots[?(@.status == 'AVAILABLE')]");
		String start = (String) free.getFirst().get("startsAt");
		String court = venue.courtId().toString();
		this.mvc.perform(get("/sites/bookhere").param("date", TOMORROW.toString()))
			.andExpect(content().string(containsString("/sites/bookhere/book?court=" + court)));

		String book = "/sites/bookhere/book";
		this.mvc.perform(get(book).param("court", court).param("start", start))
			.andExpect(status().isOk()).andExpect(content().string(containsString("Your phone number")));

		// Sign in with a texted code; a new number is asked for a name
		String phone = TestUsers.nextLocalPhone();
		this.mvc.perform(post(book + "/code").param("court", court).param("start", start).param("phone", phone))
			.andExpect(content().string(containsString("Enter the code")));
		String code = this.sms.lastCodeFor("+977" + phone);
		this.mvc.perform(post(book + "/verify").param("court", court).param("start", start).param("phone", phone).param("code", "000000".equals(code) ? "111111" : "000000"))
			.andExpect(content().string(containsString("role=\"alert\"")));
		this.mvc.perform(post(book + "/verify").param("court", court).param("start", start).param("phone", phone).param("code", code))
			.andExpect(content().string(containsString("Your name")));
		MockHttpServletResponse signedIn = this.mvc.perform(post(book + "/verify").param("court", court).param("start", start)
				.param("phone", phone).param("code", code).param("name", "Web Player"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrlPattern("/sites/bookhere/book?court=*"))
			.andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
			.andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
			.andReturn().getResponse();
		Cookie player = signedIn.getCookie(SiteSession.COOKIE);
		assertThat(player).isNotNull();

		this.mvc.perform(get(book).param("court", court).param("start", start).cookie(player))
			.andExpect(content().string(containsString("Book this slot?")))
			.andExpect(content().string(containsString("Sign out")));

		// Book it, then say how the advance was paid
		String location = this.mvc.perform(post(book + "/confirm").param("court", court).param("start", start).cookie(player))
			.andExpect(redirectedUrlPattern("/sites/bookhere/bookings/*"))
			.andReturn().getResponse().getRedirectedUrl();
		this.mvc.perform(get(location).cookie(player))
			.andExpect(content().string(containsString("Waiting for your advance")))
			.andExpect(content().string(containsString("to confirm")));
		this.mvc.perform(post(location + "/pay").cookie(player).param("method", "ESEWA").param("transactionId", "ES-WEB-12345").param("amount", "1"))
			.andExpect(content().string(containsString("role=\"alert\"")));
		String made = ownerSends(owner, get("/api/v1/owner/venues/" + venue.id() + "/bookings/" + location.substring(location.lastIndexOf('/') + 1)), null);
		assertThat(JsonPath.<String>read(made, "$.source")).isEqualTo("WEBSITE");
		assertThat(JsonPath.<String>read(made, "$.customer.name")).isEqualTo("Web Player");
		String due = JsonPath.read(made, "$.advanceRequired").toString();
		this.mvc.perform(post(location + "/pay").cookie(player).param("method", "ESEWA").param("transactionId", "ES-WEB-12345").param("amount", due))
			.andExpect(redirectedUrl(location + "?sent=1"));
		this.mvc.perform(get(location).cookie(player))
			.andExpect(content().string(containsString("Payment sent")))
			.andExpect(content().string(not(containsString("Send for checking"))));
		// Players get back to their bookings from the site
		this.mvc.perform(get("/sites/bookhere").cookie(player))
			.andExpect(content().string(containsString("href=\"/sites/bookhere/bookings\"")))
			.andExpect(header().string("Cache-Control", containsString("private")));
		this.mvc.perform(get("/sites/bookhere/bookings").cookie(player))
			.andExpect(content().string(containsString("Your bookings")))
			.andExpect(content().string(containsString(location)));
		// The venue sees it like any app payment
		assertThat(ownerSends(owner, get("/api/v1/owner/venues/" + venue.id() + "/payments/pending"), null)).contains("ES-WEB-12345");

		// The slot is gone for the next person
		this.mvc.perform(get(book).param("court", court).param("start", start))
			.andExpect(content().string(containsString("isn't free any more")));

		// Bookings are private to the player, and to the site they were made on
		this.mvc.perform(get(location)).andExpect(redirectedUrl("/sites/bookhere/"));
		this.mvc.perform(get(location).cookie(new Cookie(SiteSession.COOKIE, player.getValue().replaceFirst(".$", "x"))))
			.andExpect(redirectedUrl("/sites/bookhere/"));
		TestUser rival = this.users.signUp("Other Site Owner");
		TestVenue other = this.venues.active(rival, "Other Site Futsal");
		ownerSends(rival, put("/api/v1/owner/venues/" + other.id() + "/site"), "{\"template\":\"FLOODLIGHT\",\"subdomain\":\"othersite\"}");
		ownerSends(rival, post("/api/v1/owner/venues/" + other.id() + "/site/publish"), null);
		this.mvc.perform(get(location.replace("bookhere", "othersite")).cookie(player)).andExpect(status().isNotFound());

		// A genuine cookie for an account that's gone counts as signed out
		MockHttpServletResponse ghost = new MockHttpServletResponse();
		this.siteSession.signIn(UUID.randomUUID(), new MockHttpServletRequest(), ghost);
		String another = (String) free.get(1).get("startsAt");
		this.mvc.perform(get("/sites/bookhere/book").param("court", court).param("start", another).cookie(ghost.getCookie(SiteSession.COOKIE)))
			.andExpect(content().string(containsString("Your phone number")));

		this.mvc.perform(post("/sites/bookhere/sign-out").cookie(player))
			.andExpect(redirectedUrl("/sites/bookhere/"))
			.andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
	}

	@Test
	void playersWithAPasswordSignInWithItOnAVenuesSite() throws Exception {
		TestUser owner = this.users.signUp("Password Site Owner");
		TestVenue venue = this.venues.active(owner, "Password Site Futsal");
		ownerSends(owner, put("/api/v1/owner/venues/" + venue.id() + "/site"), "{\"template\":\"TURF\",\"subdomain\":\"passwordsite\"}");
		ownerSends(owner, post("/api/v1/owner/venues/" + venue.id() + "/site/publish"), null);
		String phone = TestUsers.nextLocalPhone();
		this.users.logIn(phone, "Password Player");
		String availability = this.mvc.perform(get("/api/v1/venues/" + venue.slug() + "/availability").param("date", TOMORROW.toString()))
			.andReturn().getResponse().getContentAsString();
		String start = JsonPath.<List<String>>read(availability, "$.courts[0].slots[?(@.status == 'AVAILABLE')].startsAt").getFirst();
		String court = venue.courtId().toString();
		String book = "/sites/passwordsite/book";

		this.mvc.perform(get(book).param("court", court).param("start", start))
			.andExpect(content().string(containsString("name=\"password\"")))
			.andExpect(content().string(containsString("Text me a code")));
		this.mvc.perform(post(book + "/password").param("court", court).param("start", start).param("phone", phone)
				.param("password", "not-the-password"))
			.andExpect(content().string(containsString("role=\"alert\"")))
			.andExpect(content().string(containsString("Wrong phone number or password")));
		Cookie player = this.mvc.perform(post(book + "/password").param("court", court).param("start", start)
				.param("phone", phone).param("password", TestUsers.PASSWORD))
			.andExpect(redirectedUrlPattern(book + "?court=*"))
			.andReturn().getResponse().getCookie(SiteSession.COOKIE);
		this.mvc.perform(get(book).param("court", court).param("start", start).cookie(player))
			.andExpect(content().string(containsString("Book this slot?")));
	}

	@Test
	void playersTopUpAndTakeBackTheirDepositOnAVenuesSite() throws Exception {
		TestUser owner = this.users.signUp("Deposit Site Owner");
		TestVenue venue = this.venues.active(owner, "Deposit Site Futsal");
		ownerSends(owner, put("/api/v1/owner/venues/" + venue.id() + "/policy"),
				"{\"advancePercent\":0,\"bookingHoldMinutes\":15,\"cancellationTiers\":[{\"withinHours\":12,\"feePercent\":50}],\"maxAdvanceBookingDays\":30,\"depositRequired\":true}");
		ownerSends(owner, put("/api/v1/owner/venues/" + venue.id() + "/site"), "{\"template\":\"FLOODLIGHT\",\"subdomain\":\"depositsite\"}");
		ownerSends(owner, post("/api/v1/owner/venues/" + venue.id() + "/site/publish"), null);
		TestUser account = this.users.signUp("Deposit Site Player");
		MockHttpServletResponse signedIn = new MockHttpServletResponse();
		this.siteSession.signIn(account.id(), new MockHttpServletRequest(), signedIn);
		Cookie player = signedIn.getCookie(SiteSession.COOKIE);

		// The day after tomorrow, so cancelling below is never inside the venue's 12-hour fee window
		String availability = this.mvc.perform(get("/api/v1/venues/" + venue.slug() + "/availability")
				.param("date", TOMORROW.plusDays(1).toString()))
			.andReturn().getResponse().getContentAsString();
		String start = JsonPath.<List<String>>read(availability, "$.courts[0].slots[?(@.status == 'AVAILABLE')].startsAt").getFirst();
		String court = venue.courtId().toString();
		String site = "/sites/depositsite";

		// The venue needs the deposit, so the player tops it up first
		this.mvc.perform(get(site + "/book").param("court", court).param("start", start).cookie(player))
			.andExpect(content().string(containsString("Add your booking deposit first")))
			.andExpect(content().string(not(containsString("Book this slot?"))));
		this.mvc.perform(get(site + "/deposit").param("court", court).param("start", start).cookie(player))
			.andExpect(content().string(containsString("Send Rs 1,000 to FutSlot")))
			.andExpect(content().string(not(containsString("Take your deposit back"))));
		String transaction = "DEP-SITE-" + UUID.randomUUID();
		this.mvc.perform(post(site + "/deposit").cookie(player).param("method", "ESEWA").param("transactionId", transaction)
				.param("amount", "1000").param("court", court).param("start", start))
			.andExpect(redirectedUrlPattern(site + "/deposit?sent=1&court=*"));
		this.mvc.perform(get(site + "/deposit").cookie(player))
			.andExpect(content().string(containsString("Top-up sent")));
		TestUser admin = this.users.signUpAdmin("Deposit Site Admin");
		String paymentId = JsonPath.<List<String>>read(ownerSends(admin, get("/api/v1/admin/payments/pending"), null),
				"$[?(@.transactionId == '%s')].id".formatted(transaction)).getFirst();
		ownerSends(admin, post("/api/v1/admin/payments/" + paymentId + "/approve"), null);

		// Full: they can book, and can't take it back while that game is still to come
		String location = this.mvc.perform(post(site + "/book/confirm").param("court", court).param("start", start).cookie(player))
			.andExpect(redirectedUrlPattern(site + "/bookings/*"))
			.andReturn().getResponse().getRedirectedUrl();
		this.mvc.perform(post(site + "/deposit/withdraw").cookie(player).param("method", "ESEWA").param("sendTo", "9800000009"))
			.andExpect(content().string(containsString("role=\"alert\"")))
			.andExpect(content().string(containsString("bookings you haven")));
		ownerSends(account, post("/api/v1/bookings/" + location.substring(location.lastIndexOf('/') + 1) + "/cancel"), "{}");

		this.mvc.perform(post(site + "/deposit/withdraw").cookie(player).param("method", "ESEWA").param("sendTo", " "))
			.andExpect(content().string(containsString("role=\"alert\"")));
		this.mvc.perform(post(site + "/deposit/withdraw").cookie(player).param("method", "ESEWA").param("sendTo", "9800000009"))
			.andExpect(redirectedUrl(site + "/deposit"));
		this.mvc.perform(get(site + "/deposit").cookie(player))
			.andExpect(content().string(containsString("Rs 1,000 is coming back to you")))
			.andExpect(content().string(containsString("eSewa 9800000009")))
			.andExpect(content().string(not(containsString("Send Rs 1,000 to FutSlot"))))
			.andExpect(content().string(not(containsString("Take your deposit back"))));

		// Changed their mind
		String withdrawalId = JsonPath.read(ownerSends(account, get("/api/v1/me/deposit"), null), "$.withdrawal.id");
		this.mvc.perform(post(site + "/deposit/withdrawals/" + withdrawalId + "/cancel").cookie(player))
			.andExpect(redirectedUrl(site + "/deposit"));
		this.mvc.perform(get(site + "/deposit").cookie(player))
			.andExpect(content().string(containsString("Take your deposit back")))
			.andExpect(content().string(not(containsString("coming back to you"))));
		this.mvc.perform(post(site + "/deposit/withdraw").param("method", "ESEWA").param("sendTo", "9800000009"))
			.andExpect(redirectedUrl(site + "/"));
	}

	@Test
	void bookingPagesWorkOnTheVenuesOwnAddress() throws Exception {
		TestUser owner = this.users.signUp("Host Booking Owner");
		TestVenue venue = this.venues.active(owner, "Host Booking Futsal");
		ownerSends(owner, put("/api/v1/owner/venues/" + venue.id() + "/site"), "{\"template\":\"TURF\",\"subdomain\":\"hostbook\"}");
		ownerSends(owner, post("/api/v1/owner/venues/" + venue.id() + "/site/publish"), null);

		this.mvc.perform(get("http://hostbook.localhost/book").param("court", "x").param("start", "y")).andExpect(forwardedUrl("/sites/hostbook/book"));
		this.mvc.perform(post("http://hostbook.localhost/book/confirm")).andExpect(forwardedUrl("/sites/hostbook/book/confirm"));
		this.mvc.perform(post("http://hostbook.localhost/sign-out")).andExpect(forwardedUrl("/sites/hostbook/sign-out"));
		this.mvc.perform(get("http://hostbook.localhost/bookings")).andExpect(forwardedUrl("/sites/hostbook/bookings"));
		this.mvc.perform(get("http://hostbook.localhost/deposit")).andExpect(forwardedUrl("/sites/hostbook/deposit"));
		this.mvc.perform(post("http://hostbook.localhost/deposit/withdraw")).andExpect(forwardedUrl("/sites/hostbook/deposit/withdraw"));
		String withdrawal = "/deposit/withdrawals/" + UUID.randomUUID() + "/cancel";
		this.mvc.perform(post("http://hostbook.localhost" + withdrawal)).andExpect(forwardedUrl("/sites/hostbook" + withdrawal));
		this.mvc.perform(post("http://hostbook.localhost/")).andExpect(status().isNotFound());
		this.mvc.perform(post("http://hostbook.localhost/api/v1/bookings")).andExpect(status().isNotFound());
	}

	private String ownerSends(TestUser user, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String json)
			throws Exception {
		request.header("Authorization", "Bearer " + user.accessToken());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request).andReturn().getResponse().getContentAsString();
	}

}
