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
	void bookingPagesWorkOnTheVenuesOwnAddress() throws Exception {
		TestUser owner = this.users.signUp("Host Booking Owner");
		TestVenue venue = this.venues.active(owner, "Host Booking Futsal");
		ownerSends(owner, put("/api/v1/owner/venues/" + venue.id() + "/site"), "{\"template\":\"TURF\",\"subdomain\":\"hostbook\"}");
		ownerSends(owner, post("/api/v1/owner/venues/" + venue.id() + "/site/publish"), null);

		this.mvc.perform(get("http://hostbook.localhost/book").param("court", "x").param("start", "y")).andExpect(forwardedUrl("/sites/hostbook/book"));
		this.mvc.perform(post("http://hostbook.localhost/book/confirm")).andExpect(forwardedUrl("/sites/hostbook/book/confirm"));
		this.mvc.perform(post("http://hostbook.localhost/sign-out")).andExpect(forwardedUrl("/sites/hostbook/sign-out"));
		this.mvc.perform(get("http://hostbook.localhost/bookings")).andExpect(forwardedUrl("/sites/hostbook/bookings"));
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
