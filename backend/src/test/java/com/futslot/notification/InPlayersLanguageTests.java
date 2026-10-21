package com.futslot.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.futslot.support.IntegrationTest;
import com.futslot.support.RecordingSmsSender;
import com.futslot.support.TestUsers;
import com.futslot.support.TestUsers.TestUser;
import com.futslot.support.TestVenues;
import com.futslot.support.TestVenues.TestVenue;
import com.jayway.jsonpath.JsonPath;

/** Players hear from FutSlot in their own language: texts, errors, notifications and names the server gives. */
@IntegrationTest
class InPlayersLanguageTests {

	private static final ZoneId KATHMANDU = ZoneId.of("Asia/Kathmandu");

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Autowired
	RecordingSmsSender sms;

	@Test
	void signingUpInNepaliTextsTheCodeAndKeepsTheAccountInNepali() throws Exception {
		String phone = TestUsers.nextLocalPhone();
		this.mvc.perform(post("/api/v1/auth/codes").header("Accept-Language", "ne").contentType(MediaType.APPLICATION_JSON)
				.content("{\"phone\":\"%s\",\"purpose\":\"SIGN_UP\"}".formatted(phone)))
			.andExpect(status().isAccepted());
		String text = this.sms.lastMessageTo("+977" + phone);
		String code = this.sms.lastCodeFor("+977" + phone);
		// The code stays in Latin digits so it can be typed; the minutes are in Devanagari
		assertThat(text).isEqualTo("तपाईंको FutSlot कोड " + code + " हो। यो ५ मिनेटमा सकिन्छ।");

		// Errors come back in Nepali, values written the Nepali way
		this.mvc.perform(post("/api/v1/auth/sign-up").header("Accept-Language", "ne").contentType(MediaType.APPLICATION_JSON)
				.content("{\"phone\":\"%s\",\"code\":\"%s\",\"fullName\":\"Nepali Player\",\"password\":\"short\"}".formatted(phone, code)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("WEAK_PASSWORD"))
			.andExpect(jsonPath("$.detail").value("पासवर्डमा कम्तीमा ८ अक्षर राख्नुहोस्।"));
		String tokens = this.mvc.perform(post("/api/v1/auth/sign-up").header("Accept-Language", "ne").contentType(MediaType.APPLICATION_JSON)
				.content("{\"phone\":\"%s\",\"code\":\"%s\",\"fullName\":\"Nepali Player\",\"password\":\"%s\"}".formatted(phone, code,
						TestUsers.PASSWORD)))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		this.mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + JsonPath.read(tokens, "$.accessToken")))
			.andExpect(jsonPath("$.preferredLanguage").value("ne"));

		this.mvc.perform(post("/api/v1/auth/sign-in").header("Accept-Language", "ne").contentType(MediaType.APPLICATION_JSON)
				.content("{\"phone\":\"%s\",\"password\":\"wrong-password\"}".formatted(phone)))
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
			.andExpect(jsonPath("$.detail").value("फोन नम्बर वा पासवर्ड मिलेन।"));
		// English stays as it was, and a code without Nepali wording falls back to it
		this.mvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON)
				.content("{\"phone\":\"%s\",\"password\":\"wrong-password\"}".formatted(phone)))
			.andExpect(jsonPath("$.detail").value("Wrong phone number or password"));
	}

	@Test
	void playersAreNotifiedInTheirLanguageAndVenuesInEnglish() throws Exception {
		TestUser owner = this.users.signUp("Bhasha Owner");
		TestVenue venue = this.venues.active(owner, "Bhasha Futsal");
		String phone = TestUsers.nextLocalPhone();
		TestUser player = this.users.logIn(phone, "Bhasha Player");
		send(player, patch("/api/v1/me"), "{\"preferredLanguage\":\"ne\"}").andExpect(status().isOk());

		OffsetDateTime at = LocalDate.now(KATHMANDU).plusDays(2).atTime(18, 0).atZone(KATHMANDU).toOffsetDateTime()
			.truncatedTo(ChronoUnit.MINUTES);
		send(owner, post("/api/v1/owner/venues/" + venue.id() + "/bookings"), """
				{"courtId":"%s","startsAt":"%s","endsAt":"%s","customerName":"Bhasha Player","customerPhone":"%s",
				 "source":"PHONE","price":1500}""".formatted(venue.courtId(), at, at.plusHours(1), "+977" + phone))
			.andExpect(status().isCreated());

		String inbox = send(player, get("/api/v1/notifications"), null).andReturn().getResponse().getContentAsString();
		assertThat(JsonPath.<String>read(inbox, "$.content[0].title")).isEqualTo("Bhasha Futsal ले तपाईंका लागि स्लट बुक गर्‍यो");
		assertThat(JsonPath.<String>read(inbox, "$.content[0].body")).contains("बेलुका ६ बजे", "रेफ ").doesNotContain("18:00");

		// The player books in the app: they're told in Nepali, the venue in English
		send(player, post("/api/v1/bookings"), "{\"courtId\":\"%s\",\"startsAt\":\"%s\"}".formatted(venue.courtId(), at.plusHours(2)))
			.andExpect(status().isCreated());
		inbox = send(player, get("/api/v1/notifications"), null).andReturn().getResponse().getContentAsString();
		assertThat(JsonPath.<String>read(inbox, "$.content[0].title")).isIn("बुकिङ पक्का भयो", "भुक्तानी पूरा गर्नुहोस्");
		String owners = send(owner, get("/api/v1/notifications"), null).andReturn().getResponse().getContentAsString();
		assertThat(JsonPath.<String>read(owners, "$.content[0].title")).isEqualTo("New booking");
	}

	@Test
	void aSplitGameStartsWithSidesNamedInTheBookersLanguage() throws Exception {
		TestVenue venue = this.venues.active(this.users.signUp("Pakshya Owner"), "Pakshya Futsal");
		TestUser booker = this.users.signUp("Pakshya Booker");
		OffsetDateTime at = LocalDate.now(KATHMANDU).plusDays(2).atTime(7, 0).atZone(KATHMANDU).toOffsetDateTime();
		String bookingId = JsonPath.read(send(booker, post("/api/v1/bookings"),
				"{\"courtId\":\"%s\",\"startsAt\":\"%s\"}".formatted(venue.courtId(), at))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");

		send(booker, put("/api/v1/bookings/" + bookingId + "/split").header("Accept-Language", "ne"), "{\"rule\":\"EQUAL\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sideAName").value("टिम क"))
			.andExpect(jsonPath("$.sideBName").value("टिम ख"));
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request, String json) throws Exception {
		request.header("Authorization", "Bearer " + user.accessToken());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request);
	}

}
