package com.futslot.site;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.futslot.support.IntegrationTest;
import com.futslot.support.TestUsers;
import com.futslot.support.TestUsers.TestUser;
import com.futslot.support.TestVenues;
import com.futslot.support.TestVenues.TestVenue;

@IntegrationTest
class DirectoryTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Test
	void citiesAreasAndVenuesHaveWholePagesForSearchEngines() throws Exception {
		TestVenue venue = venueIn("Directory Arena", "Dirtole Chowk", "Dirpur");
		String home = "http://localhost:8080";

		this.mvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
			.andExpect(content().string(containsString("<h1>Book a futsal court</h1>")))
			.andExpect(content().string(containsString("<link rel=\"canonical\" href=\"" + home + "/\">")))
			.andExpect(content().string(containsString("<link rel=\"alternate\" hreflang=\"ne\" href=\"" + home + "/ne/\">")))
			.andExpect(content().string(containsString("\"@type\":\"WebSite\"")))
			.andExpect(content().string(containsString("href=\"/futsal/dirpur\"")))
			.andExpect(content().string(containsString("href=\"/venues/" + venue.slug() + "\"")))
			// Its next free hours, which book straight away
			.andExpect(content().string(containsString("class=\"lit\" href=\"/venues/" + venue.slug() + "/book?court=" + venue.courtId())))
			.andExpect(content().string(containsString("<meta property=\"og:image\" content=\"" + home + "/site-assets/futslot-share.jpg\">")))
			.andExpect(content().string(not(containsString("noindex"))));

		this.mvc.perform(get("/futsal/dirpur"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("<title>Futsal in Dirpur: book a court online | FutSlot</title>")))
			.andExpect(content().string(containsString("<h1>Futsal in Dirpur</h1>")))
			.andExpect(content().string(containsString("1 futsal venue in Dirpur with 1 court, from Rs 1,200 a game.")))
			.andExpect(content().string(containsString("<link rel=\"canonical\" href=\"" + home + "/futsal/dirpur\">")))
			.andExpect(content().string(containsString("href=\"/futsal/dirpur/dirtole-chowk\"")))
			.andExpect(content().string(containsString("\"@type\":\"BreadcrumbList\"")))
			.andExpect(content().string(containsString("Directory Arena")));
		this.mvc.perform(get("/futsal/dirpur/dirtole-chowk"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("<h1>Futsal in Dirtole Chowk, Dirpur</h1>")))
			.andExpect(content().string(containsString("\"item\":\"" + home + "/futsal/dirpur\"")));
		this.mvc.perform(get("/futsal/nowhere")).andExpect(status().isNotFound());
		this.mvc.perform(get("/futsal/dirpur/nowhere")).andExpect(status().isNotFound());

		// A venue's page: its details and free slots, which book in the player app
		this.mvc.perform(get("/venues/" + venue.slug()))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("<title>Directory Arena, Dirtole Chowk: futsal booking | FutSlot</title>")))
			.andExpect(content().string(containsString("<link rel=\"canonical\" href=\"" + home + "/venues/" + venue.slug() + "\">")))
			.andExpect(content().string(containsString("\"@type\":\"SportsActivityLocation\"")))
			.andExpect(content().string(containsString("\"url\":\"" + home + "/venues/" + venue.slug() + "\"")))
			.andExpect(content().string(containsString("href=\"/venues/" + venue.slug() + "/book?court=" + venue.courtId())))
			.andExpect(content().string(containsString("href=\"/venues/" + venue.slug() + "/slots\"")))
			.andExpect(content().string(containsString("More futsal in Dirtole Chowk")));
		this.mvc.perform(get("/venues/no-such-venue")).andExpect(status().isNotFound());

		// The same in Nepali, beneath /ne
		this.mvc.perform(get("/ne/venues/" + venue.slug()))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("lang=\"ne\"")))
			.andExpect(content().string(containsString("<link rel=\"canonical\" href=\"" + home + "/ne/venues/" + venue.slug() + "\">")))
			.andExpect(content().string(containsString("फुटसल बुकिङ | FutSlot")));
		this.mvc.perform(get("/ne/futsal/dirpur"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("<h1>Dirpur मा फुटसल</h1>")))
			.andExpect(content().string(containsString("href=\"/ne/futsal/dirpur/dirtole-chowk\"")));
		this.mvc.perform(get("/ne/")).andExpect(status().isOk()).andExpect(content().string(containsString("फुटसल कोर्ट बुक गर्नुहोस्")));

		this.mvc.perform(get("/sitemap.xml"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("<loc>" + home + "/</loc>")))
			.andExpect(content().string(containsString("<loc>" + home + "/futsal/dirpur/dirtole-chowk</loc>")))
			.andExpect(content().string(containsString("<loc>" + home + "/ne/venues/" + venue.slug() + "</loc>")))
			.andExpect(content().string(containsString("hreflang=\"en\" href=\"" + home + "/venues/" + venue.slug() + "\"")));
		this.mvc.perform(get("/robots.txt"))
			.andExpect(content().string(containsString("Sitemap: " + home + "/sitemap.xml")));
	}

	@Test
	void venueWebsitesLinkBackToFutslot() throws Exception {
		TestUser owner = this.users.signUp("Backlink Owner");
		TestVenue venue = this.venues.active(owner, "Backlink Futsal");
		String site = "/api/v1/owner/venues/" + venue.id() + "/site";
		this.mvc.perform(put(site).header("Authorization", owner.bearer()).contentType(MediaType.APPLICATION_JSON)
			.content("{\"template\":\"TURF\",\"subdomain\":\"backlink\"}")).andExpect(status().isOk());
		this.mvc.perform(get("/site-preview/" + venue.slug()))
			.andExpect(content().string(containsString("<a href=\"http://localhost:8080/\">FutSlot</a>")));
	}

	@Test
	void namesBecomeAddresses() {
		assertThat(DirectoryService.slug(" New Baneshwor ")).isEqualTo("new-baneshwor");
		assertThat(DirectoryService.slug("Thimi (Madhyapur)")).isEqualTo("thimi-madhyapur");
		assertThat(DirectoryService.slug("Jhamsikhel")).isEqualTo("jhamsikhel");
		assertThat(DirectoryService.slug("काठमाडौं")).isEmpty();
	}

	private TestVenue venueIn(String name, String area, String city) throws Exception {
		TestUser owner = this.users.signUp(name + " Owner");
		TestVenue venue = this.venues.active(owner, name);
		this.mvc.perform(put("/api/v1/owner/venues/" + venue.id()).header("Authorization", owner.bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name":"%s","addressLine":"Ring Road","area":"%s","city":"%s"}""".formatted(name, area, city)))
			.andExpect(status().isOk());
		return venue;
	}

}
