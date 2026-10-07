package com.futslot.chat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;

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
import com.jayway.jsonpath.JsonPath;

@IntegrationTest
class ChatFlowTests {

	private static final LocalDate GAME_DAY = LocalDate.now(ZoneId.of("Asia/Kathmandu")).plusDays(3);

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Test
	void playersAndVenuesTalkAndSeeWhatsUnread() throws Exception {
		TestUser owner = this.users.signUp("Chat Owner");
		TestVenue venue = this.venues.active(owner, "Chat Futsal");
		String deskPhone = TestUsers.nextLocalPhone();
		send(owner, post("/api/v1/owner/venues/" + venue.id() + "/staff"),
				"{\"phone\":\"%s\",\"fullName\":\"Desk Didi\",\"role\":\"RECEPTIONIST\"}".formatted(deskPhone))
			.andExpect(status().isCreated());
		TestUser desk = this.users.logIn(deskPhone, "Desk Didi");
		TestUser player = this.users.signUp("Chatty Player");
		String bookingId = JsonPath.read(send(player, post("/api/v1/bookings"),
				"{\"courtId\":\"%s\",\"startsAt\":\"%sT19:00:00+05:45\"}".formatted(venue.courtId(), GAME_DAY))
			.andReturn().getResponse().getContentAsString(), "$.id");

		send(player, post("/api/v1/venues/" + venue.id() + "/messages"),
				"{\"body\":\"  Can we bring our own ball?  \",\"bookingId\":\"%s\"}".formatted(bookingId))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.body").value("Can we bring our own ball?"))
			.andExpect(jsonPath("$.fromYou").value(true))
			.andExpect(jsonPath("$.bookingId").value(bookingId));
		for (TestUser staff : new TestUser[] { owner, desk }) {
			send(staff, get("/api/v1/notifications"), null)
				.andExpect(jsonPath("$.content[0].type").value("CHAT_MESSAGE"))
				.andExpect(jsonPath("$.content[0].title").value("Chatty P."));
		}
		send(player, get("/api/v1/notifications"), null).andExpect(jsonPath("$.content[0].type").value("BOOKING_CONFIRMED"));

		String conversationId = JsonPath.read(send(owner, get("/api/v1/owner/venues/" + venue.id() + "/conversations"), null)
			.andExpect(jsonPath("$[0].playerName").value("Chatty Player"))
			.andExpect(jsonPath("$[0].unread").value(1))
			.andExpect(jsonPath("$[0].lastMessage").value("Can we bring our own ball?"))
			.andReturn().getResponse().getContentAsString(), "$[0].id");
		String venueThread = "/api/v1/owner/venues/" + venue.id() + "/conversations/" + conversationId;
		send(desk, post(venueThread + "/messages"), "{\"body\":\"Of course, see you at 7!\"}").andExpect(status().isCreated())
			.andExpect(jsonPath("$.senderName").value("Desk Didi (Chat Futsal)"))
			.andExpect(jsonPath("$.fromYou").value(true));
		send(desk, post(venueThread + "/read"), null).andExpect(status().isNoContent());
		send(owner, get("/api/v1/owner/venues/" + venue.id() + "/conversations"), null)
			.andExpect(jsonPath("$[0].unread").value(0));

		// The player sees the venue, not the staff member
		send(player, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("CHAT_MESSAGE"))
			.andExpect(jsonPath("$.content[0].title").value("Chat Futsal"));
		send(player, get("/api/v1/me/conversations"), null)
			.andExpect(jsonPath("$[0].venueName").value("Chat Futsal"))
			.andExpect(jsonPath("$[0].unread").value(1));
		send(player, get("/api/v1/me/conversations/" + conversationId + "/messages"), null)
			.andExpect(jsonPath("$[0].senderName").value("Chat Futsal"))
			.andExpect(jsonPath("$[0].fromYou").value(false))
			.andExpect(jsonPath("$[1].readAt").exists());
		send(player, post("/api/v1/me/conversations/" + conversationId + "/read"), null);
		send(player, get("/api/v1/me/conversations"), null).andExpect(jsonPath("$[0].unread").value(0));

		send(this.users.signUp("Eavesdropper"), get("/api/v1/me/conversations/" + conversationId + "/messages"), null)
			.andExpect(status().isNotFound());
		TestUser otherOwner = this.users.signUp("Other Chat Owner");
		TestVenue other = this.venues.active(otherOwner, "Other Chat Futsal");
		send(otherOwner, get("/api/v1/owner/venues/" + other.id() + "/conversations/" + conversationId + "/messages"), null)
			.andExpect(status().isNotFound());
		send(player, post("/api/v1/venues/" + other.id() + "/messages"),
				"{\"body\":\"Wrong place\",\"bookingId\":\"%s\"}".formatted(bookingId))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_BOOKING"));
	}

	@Test
	void longThreadsComeInPages() throws Exception {
		TestVenue venue = this.venues.active(this.users.signUp("Paging Owner"), "Paging Futsal");
		TestUser player = this.users.signUp("Paging Player");
		for (int i = 1; i <= 52; i++) {
			send(player, post("/api/v1/venues/" + venue.id() + "/messages"), "{\"body\":\"Message %d\"}".formatted(i))
				.andExpect(status().isCreated());
		}
		String conversationId = JsonPath.read(send(player, get("/api/v1/me/conversations"), null)
			.andReturn().getResponse().getContentAsString(), "$[0].id");
		String first = send(player, get("/api/v1/me/conversations/" + conversationId + "/messages"), null)
			.andExpect(jsonPath("$.length()").value(50))
			.andExpect(jsonPath("$[0].body").value("Message 52"))
			.andReturn().getResponse().getContentAsString();
		String oldest = JsonPath.read(first, "$[49].createdAt");
		send(player, get("/api/v1/me/conversations/" + conversationId + "/messages").param("before", oldest), null)
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[1].body").value("Message 1"));
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request, String json) throws Exception {
		request.header("Authorization", user.bearer());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request);
	}

}
