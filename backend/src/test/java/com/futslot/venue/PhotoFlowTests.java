package com.futslot.venue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
class PhotoFlowTests {

	/** The first bytes of a PNG; enough for the upload check. */
	private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D };

	private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10 };

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Test
	void ownersUploadPhotosAndTheFirstIsTheCover() throws Exception {
		TestUser owner = this.users.signUp("Photo Owner");
		TestVenue venue = this.venues.active(owner, "Photo Futsal");

		upload(owner, venue, "notes.png", "just text".getBytes(StandardCharsets.UTF_8)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("UNSUPPORTED_IMAGE"));
		String firstUrl = JsonPath.read(upload(owner, venue, "turf.png", PNG).andExpect(status().isCreated())
			.andExpect(jsonPath("$.cover").value(true))
			.andExpect(jsonPath("$.url", startsWith("/media/venues/" + venue.id() + "/")))
			.andReturn().getResponse().getContentAsString(), "$.url");
		String second = JsonPath.read(upload(owner, venue, "night.jpg", JPEG).andExpect(jsonPath("$.cover").value(false))
			.andReturn().getResponse().getContentAsString(), "$.id");

		this.mvc.perform(get(firstUrl)).andExpect(status().isOk())
			.andExpect(header().string("Content-Type", "image/png"))
			.andExpect(content().bytes(PNG));
		this.mvc.perform(get("/media/../application.properties")).andExpect(status().is4xxClientError());
		this.mvc.perform(get("/api/v1/venues/" + venue.slug()))
			.andExpect(jsonPath("$.photos.length()").value(2))
			.andExpect(jsonPath("$.photos[0].url").value(firstUrl));
		this.mvc.perform(get("/api/v1/venues").param("q", "Photo Futsal"))
			.andExpect(jsonPath("$.content[0].coverPhotoUrl").value(firstUrl));

		send(owner, put("/api/v1/owner/venues/" + venue.id() + "/photos/" + second),
				"{\"cover\":true,\"caption\":\"Under lights\"}")
			.andExpect(jsonPath("$[0].id").value(second))
			.andExpect(jsonPath("$[0].caption").value("Under lights"))
			.andExpect(jsonPath("$[1].cover").value(false));

		// Deleting the cover promotes the next photo, and the file goes too
		String coverUrl = JsonPath.read(send(owner, get("/api/v1/owner/venues/" + venue.id() + "/photos"), null)
			.andReturn().getResponse().getContentAsString(), "$[0].url");
		send(owner, delete("/api/v1/owner/venues/" + venue.id() + "/photos/" + second), null)
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].cover").value(true));
		this.mvc.perform(get(coverUrl)).andExpect(status().isNotFound());

		upload(this.users.signUp("Photo Bomber"), venue, "x.png", PNG).andExpect(status().isNotFound());
	}

	@Test
	void photosUploadedAtTheSameTimeGetOneCoverAndTheirOwnPlaces() throws Exception {
		TestUser owner = this.users.signUp("Busy Uploader");
		TestVenue venue = this.venues.active(owner, "Parallel Futsal");

		try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
			List<Future<Integer>> statuses = new ArrayList<>();
			for (int i = 0; i < 4; i++) {
				String name = "court-" + i + ".png";
				statuses.add(pool.submit(() -> upload(owner, venue, name, PNG).andReturn().getResponse().getStatus()));
			}
			for (Future<Integer> status : statuses) {
				assertThat(status.get()).isEqualTo(201);
			}
		}
		String photos = send(owner, get("/api/v1/owner/venues/" + venue.id() + "/photos"), null)
			.andExpect(jsonPath("$.length()").value(4))
			.andExpect(jsonPath("$[*].sortOrder", containsInAnyOrder(0, 1, 2, 3)))
			.andReturn().getResponse().getContentAsString();
		List<Boolean> covers = JsonPath.read(photos, "$[*].cover");
		assertThat(covers).containsExactly(true, false, false, false);
	}

	private ResultActions upload(TestUser user, TestVenue venue, String name, byte[] bytes) throws Exception {
		return this.mvc.perform(multipart("/api/v1/owner/venues/" + venue.id() + "/photos")
			.file(new MockMultipartFile("file", name, "image/png", bytes))
			.header("Authorization", user.bearer()));
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request, String json) throws Exception {
		request.header("Authorization", user.bearer());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request);
	}

}
