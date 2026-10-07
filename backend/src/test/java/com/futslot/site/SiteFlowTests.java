package com.futslot.site;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.futslot.support.IntegrationTest;
import com.futslot.support.TestUsers;
import com.futslot.support.TestUsers.TestUser;
import com.futslot.support.TestVenues;
import com.futslot.support.TestVenues.TestVenue;

@IntegrationTest
class SiteFlowTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Test
	void ownersChooseATemplateAndAddressThenPublish() throws Exception {
		TestUser owner = this.users.signUp("Site Owner");
		TestVenue venue = this.venues.active(owner, "Sitewise Futsal");
		String site = "/api/v1/owner/venues/" + venue.id() + "/site";

		send(owner, get(site), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.saved").value(false))
			.andExpect(jsonPath("$.template").value("TURF"))
			.andExpect(jsonPath("$.subdomain").value(venue.slug()))
			.andExpect(jsonPath("$.live").value(true));
		send(owner, post(site + "/publish"), null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SITE_NOT_SET_UP"));

		send(owner, put(site), "{\"template\":\"FLOODLIGHT\",\"subdomain\":\"admin\"}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("SUBDOMAIN_UNAVAILABLE"));
		send(owner, put(site), "{\"template\":\"FLOODLIGHT\",\"subdomain\":\"-bad-\"}").andExpect(status().isBadRequest());
		send(owner, put(site), "{\"template\":\"FLOODLIGHT\",\"subdomain\":\"sitewise\",\"accentColor\":\"green\"}")
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_COLOR"));
		send(owner, put(site), "{\"template\":\"FLOODLIGHT\",\"subdomain\":\"sitewise\",\"customDomain\":\"x.localhost\"}")
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_DOMAIN"));
		send(owner, put(site), """
				{"template":"FLOODLIGHT","subdomain":" SiteWise ","customDomain":"https://www.SitewiseFutsal.com/home",
				 "accentColor":"#C6F432","headline":"Night games in Patan"}""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.saved").value(true))
			.andExpect(jsonPath("$.subdomain").value("sitewise"))
			.andExpect(jsonPath("$.customDomain").value("sitewisefutsal.com"))
			.andExpect(jsonPath("$.accentColor").value("#c6f432"))
			.andExpect(jsonPath("$.url").value("http://sitewise.localhost:8080"))
			.andExpect(jsonPath("$.published").value(false));

		// Another venue can't take the same address or domain
		TestUser rival = this.users.signUp("Rival Site Owner");
		TestVenue other = this.venues.active(rival, "Rival Sitewise");
		send(rival, get("/api/v1/owner/venues/" + other.id() + "/site/subdomain-check").param("subdomain", "sitewise"), null)
			.andExpect(jsonPath("$.available").value(false));
		send(rival, put("/api/v1/owner/venues/" + other.id() + "/site"), "{\"template\":\"TURF\",\"subdomain\":\"rival\",\"customDomain\":\"sitewisefutsal.com\"}")
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DOMAIN_TAKEN"));

		// Not live until published
		this.mvc.perform(get("/sites/sitewise")).andExpect(status().isNotFound());
		this.mvc.perform(get("/internal/sites/domain-check").param("domain", "sitewisefutsal.com")).andExpect(status().isNotFound());
		send(owner, post(site + "/publish"), null).andExpect(jsonPath("$.published").value(true));

		this.mvc.perform(get("/sites/sitewise"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
			.andExpect(content().string(containsString("Sitewise Futsal")))
			.andExpect(content().string(containsString("Night games in Patan")))
			.andExpect(content().string(containsString("class=\"floodlight\"")))
			.andExpect(content().string(containsString("<link rel=\"canonical\" href=\"https://sitewisefutsal.com/\">")))
			.andExpect(content().string(containsString("\"@type\":\"SportsActivityLocation\"")))
			.andExpect(content().string(containsString("--accent: #c6f432")))
			.andExpect(content().string(not(containsString("noindex"))));
		this.mvc.perform(get("/sites/sitewise/robots.txt"))
			.andExpect(content().string(containsString("Sitemap: https://sitewisefutsal.com/sitemap.xml")));
		this.mvc.perform(get("/sites/sitewise/sitemap.xml")).andExpect(content().string(containsString("<loc>https://sitewisefutsal.com/</loc>")));
		this.mvc.perform(get("/internal/sites/domain-check").param("domain", "www.sitewisefutsal.com")).andExpect(status().isOk());
		this.mvc.perform(get("/internal/sites/domain-check").param("domain", "sitewise.localhost")).andExpect(status().isOk());
		this.mvc.perform(get("/internal/sites/domain-check").param("domain", "evil.example.com")).andExpect(status().isNotFound());

		// Previews work for any template and stay out of search results
		this.mvc.perform(get("/site-preview/" + venue.slug()).param("template", "turf").param("accent", "1d4ed8"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("class=\"turf\"")))
			.andExpect(content().string(containsString("noindex")))
			.andExpect(content().string(containsString("--accent: #1d4ed8")))
			.andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));

		send(owner, post(site + "/unpublish"), null).andExpect(jsonPath("$.published").value(false));
		this.mvc.perform(get("/sites/sitewise")).andExpect(status().isNotFound());
	}

	@Test
	void aVenuesOwnAddressShowsItsSite() throws Exception {
		TestUser owner = this.users.signUp("Host Owner");
		TestVenue venue = this.venues.active(owner, "Host Futsal");
		String site = "/api/v1/owner/venues/" + venue.id() + "/site";
		send(owner, put(site), "{\"template\":\"TURF\",\"subdomain\":\"hostfutsal\",\"customDomain\":\"hostfutsal.com.np\"}");
		send(owner, post(site + "/publish"), null).andExpect(status().isOk());

		this.mvc.perform(get("http://hostfutsal.localhost/")).andExpect(forwardedUrl("/sites/hostfutsal"));
		this.mvc.perform(get("http://www.hostfutsal.com.np/sitemap.xml")).andExpect(forwardedUrl("/sites/hostfutsal/sitemap.xml"));
		this.mvc.perform(get("http://hostfutsal.localhost/api/v1/me")).andExpect(status().isNotFound());
		// Photos and styles load from the venue's address too; FutSlot's own addresses are untouched
		this.mvc.perform(get("http://hostfutsal.localhost/site-assets/turf.css")).andExpect(status().isOk());
		this.mvc.perform(get("http://www.localhost/actuator/health")).andExpect(status().isOk());
		this.mvc.perform(get("http://api.localhost/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void lateNightPricesSplitAtMidnightReadAsOne() throws Exception {
		TestUser owner = this.users.signUp("Late Owner");
		TestVenue venue = this.venues.active(owner, "Late Night Futsal");
		String rules = "/api/v1/owner/venues/" + venue.id() + "/courts/" + venue.courtId() + "/pricing-rules";
		send(owner, post(rules), "{\"label\":\"PEAK\",\"dayOfWeek\":\"FRIDAY\",\"startTime\":\"00:00\",\"endTime\":\"01:00\",\"price\":1500}")
			.andExpect(status().isCreated());
		send(owner, post(rules), "{\"label\":\"PEAK\",\"dayOfWeek\":\"FRIDAY\",\"startTime\":\"22:00\",\"endTime\":\"23:59:59\",\"price\":1500}")
			.andExpect(status().isCreated());

		this.mvc.perform(get("/site-preview/" + venue.slug()))
			.andExpect(content().string(containsString("Fridays, 10 PM – 1 AM")))
			.andExpect(content().string(not(containsString("Fridays, 12 AM – 1 AM"))));
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request, String json) throws Exception {
		request.header("Authorization", "Bearer " + user.accessToken());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request);
	}

}
