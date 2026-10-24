package com.futslot.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CloudinaryMediaStorageTests {

	private static final String UPLOADED = "https://res.cloudinary.com/demo/image/upload/v1761300000/futslot/venues/v1/a.png";

	private final RestClient.Builder http = RestClient.builder();

	private final MockRestServiceServer cloudinary = MockRestServiceServer.bindTo(this.http).build();

	private final CloudinaryMediaStorage storage = new CloudinaryMediaStorage("cloudinary://123456:s3cret@demo", "futslot",
			this.http, Clock.fixed(Instant.ofEpochSecond(1315060510), ZoneOffset.UTC));

	@Test
	void uploadsSignedAndReturnsCloudinarysAddress() {
		// Signature: sha1("public_id=futslot/venues/v1/a&timestamp=1315060510" + "s3cret")
		this.cloudinary.expect(requestTo("https://api.cloudinary.com/v1_1/demo/image/upload"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
			.andExpect(content().multipartDataContains(Map.of("public_id", "futslot/venues/v1/a", "timestamp", "1315060510",
					"api_key", "123456", "signature", "ebf62c171b29f1bb7be2d041336bd510404f8bdf", "file", new byte[] { 1, 2, 3 })))
			.andRespond(withSuccess("{\"secure_url\":\"" + UPLOADED + "\"}", MediaType.APPLICATION_JSON));

		assertThat(this.storage.store("venues/v1/a.png", new byte[] { 1, 2, 3 })).isEqualTo(UPLOADED);
		this.cloudinary.verify();
	}

	@Test
	void deletesByTheKeyBehindItsAddress() {
		this.cloudinary.expect(requestTo("https://api.cloudinary.com/v1_1/demo/image/destroy"))
			.andExpect(content().formDataContains(Map.of("public_id", "futslot/venues/v1/a")))
			.andRespond(withSuccess("{\"result\":\"ok\"}", MediaType.APPLICATION_JSON));

		String key = this.storage.keyOf(UPLOADED).orElseThrow();
		assertThat(key).isEqualTo("venues/v1/a.png");
		this.storage.delete(key);
		this.cloudinary.verify();
	}

	@Test
	void onlyItsOwnAddressesHaveKeys() {
		assertThat(this.storage.keyOf("/media/venues/v1/a.png")).isEmpty();
		assertThat(this.storage.keyOf("https://res.cloudinary.com/other/image/upload/v1/futslot/venues/v1/a.png")).isEmpty();
		assertThat(this.storage.keyOf("https://res.cloudinary.com/demo/image/upload/v1/elsewhere/a.png")).isEmpty();
		assertThat(this.storage.read("venues/v1/a.png")).isEmpty();
	}

	@Test
	void refusesAnAddressThatIsntCloudinarys() {
		assertThatThrownBy(() -> new CloudinaryMediaStorage("https://demo", "futslot", RestClient.builder(), Clock.systemUTC()))
			.isInstanceOf(IllegalArgumentException.class);
	}

}
