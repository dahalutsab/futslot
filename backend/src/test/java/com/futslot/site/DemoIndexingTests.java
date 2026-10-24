package com.futslot.site;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.futslot.support.IntegrationTest;
import com.futslot.support.TestUsers;
import com.futslot.support.TestUsers.TestUser;
import com.futslot.support.TestVenues;
import com.futslot.support.TestVenues.TestVenue;

/** A demo's venues are made up, so with search indexing off only FutSlot's front page is for search engines. */
@IntegrationTest
@TestPropertySource(properties = "futslot.sites.search-indexing=false")
class DemoIndexingTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Test
	void venuesStayOutOfSearchResults() throws Exception {
		TestUser owner = this.users.signUp("Demo Owner");
		TestVenue venue = this.venues.active(owner, "Made Up Futsal");
		String site = "/api/v1/owner/venues/" + venue.id() + "/site";
		this.mvc.perform(put(site).header("Authorization", owner.bearer()).contentType(MediaType.APPLICATION_JSON)
			.content("{\"template\":\"TURF\",\"subdomain\":\"madeup\"}")).andExpect(status().isOk());
		this.mvc.perform(post(site + "/publish").header("Authorization", owner.bearer())).andExpect(status().isOk());

		this.mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(not(containsString("noindex"))));
		this.mvc.perform(get("/venues/" + venue.slug())).andExpect(content().string(containsString("<meta name=\"robots\" content=\"noindex\">")));
		this.mvc.perform(get("/futsal/kathmandu")).andExpect(content().string(containsString("<meta name=\"robots\" content=\"noindex\">")));
		this.mvc.perform(get("/sites/madeup")).andExpect(content().string(containsString("noindex")));
		this.mvc.perform(get("/sitemap.xml"))
			.andExpect(content().string(containsString("<loc>http://localhost:8080/</loc>")))
			.andExpect(content().string(not(containsString("/venues/"))))
			.andExpect(content().string(not(containsString("/futsal/"))));
	}

}
