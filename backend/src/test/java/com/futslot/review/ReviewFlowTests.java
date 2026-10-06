package com.futslot.review;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;

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
class ReviewFlowTests {

	private static final LocalDate GAME_DAY = LocalDate.now(ZoneId.of("Asia/Kathmandu")).plusDays(3);

	@Autowired
	MockMvc mvc;

	@Autowired
	TestUsers users;

	@Autowired
	TestVenues venues;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void playerReviewsAPlayedBookingAndTheVenueRatingFollows() throws Exception {
		TestUser owner = this.users.signUp("Review Owner");
		TestVenue venue = this.venues.active(owner, "Review Futsal");
		TestUser player = this.users.signUp("Ram Bahadur Shrestha");
		String bookingId = played(player, venue, "07:00", 2);

		send(player, get("/api/v1/me/reviews/pending"), null).andExpect(jsonPath("$[*].id", hasItem(bookingId)));
		writeReview(player, bookingId, 4, "  Good turf,   friendly staff  ").andExpect(status().isCreated())
			.andExpect(jsonPath("$.rating").value(4))
			.andExpect(jsonPath("$.comment").value("Good turf,   friendly staff"))
			.andExpect(jsonPath("$.reviewerName").value("Ram S."))
			.andExpect(jsonPath("$.bookingId").value(bookingId))
			.andExpect(jsonPath("$.playedOn").value(GAME_DAY.minusDays(5).toString()));
		send(player, get("/api/v1/me/reviews/pending"), null).andExpect(jsonPath("$[*].id", not(hasItem(bookingId))));

		this.mvc.perform(get("/api/v1/venues/" + venue.slug()))
			.andExpect(jsonPath("$.averageRating").value(4.0))
			.andExpect(jsonPath("$.reviewCount").value(1));
		this.mvc.perform(get("/api/v1/venues/" + venue.slug() + "/reviews"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].reviewerName").value("Ram S."))
			.andExpect(jsonPath("$.content[0].bookingId").value(nullValue()));
		send(owner, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("NEW_REVIEW"))
			.andExpect(jsonPath("$.content[0].title").value("New 4-star review"))
			.andExpect(jsonPath("$.content[0].body").value(containsString("\"Good turf, friendly staff\"")));

		// A second player's rating averages in; editing and deleting recompute it
		TestUser other = this.users.signUp("Sita");
		String otherBooking = played(other, venue, "08:00", 2);
		writeReview(other, otherBooking, 1, null).andExpect(status().isCreated());
		this.mvc.perform(get("/api/v1/venues/" + venue.slug() + "/reviews/summary"))
			.andExpect(jsonPath("$.averageRating").value(2.5))
			.andExpect(jsonPath("$.reviewCount").value(2))
			.andExpect(jsonPath("$.stars['4']").value(1))
			.andExpect(jsonPath("$.stars['1']").value(1))
			.andExpect(jsonPath("$.stars['5']").value(0));
		send(other, put("/api/v1/bookings/" + otherBooking + "/review"), "{\"rating\":3}").andExpect(status().isOk());
		this.mvc.perform(get("/api/v1/venues/" + venue.slug())).andExpect(jsonPath("$.averageRating").value(3.5));
		this.mvc.perform(get("/api/v1/venues/" + venue.slug() + "/reviews").param("sort", "LOWEST"))
			.andExpect(jsonPath("$.content[*].rating", contains(3, 4)));
		send(other, delete("/api/v1/bookings/" + otherBooking + "/review"), null).andExpect(status().isNoContent());
		this.mvc.perform(get("/api/v1/venues/" + venue.slug()))
			.andExpect(jsonPath("$.averageRating").value(4.0))
			.andExpect(jsonPath("$.reviewCount").value(1));
		send(other, get("/api/v1/bookings/" + otherBooking + "/review"), null).andExpect(status().isNotFound());
	}

	@Test
	void onlyPlayedBookingsInTheLastMonthCanBeReviewedOnce() throws Exception {
		TestUser owner = this.users.signUp("Strict Review Owner");
		TestVenue venue = this.venues.active(owner, "Strict Review Futsal");
		TestUser player = this.users.signUp("Strict Reviewer");

		String upcoming = id(book(player, venue, "07:00"));
		writeReview(player, upcoming, 5, null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("NOT_REVIEWABLE"));

		String played = played(player, venue, "08:00", 2);
		writeReview(this.users.signUp("Not There"), played, 5, null).andExpect(status().isNotFound());
		writeReview(player, played, 6, null).andExpect(status().isBadRequest());
		writeReview(player, played, 5, null).andExpect(status().isCreated());
		writeReview(player, played, 4, null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_REVIEWED"));

		String longAgo = played(player, venue, "09:00", 40);
		writeReview(player, longAgo, 5, null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("REVIEW_WINDOW_CLOSED"));
		send(player, get("/api/v1/me/reviews/pending"), null).andExpect(jsonPath("$[*].id", not(hasItem(longAgo))));
	}

	@Test
	void venueRepliesButCannotHideReviews() throws Exception {
		TestUser owner = this.users.signUp("Reply Owner");
		TestVenue venue = this.venues.active(owner, "Reply Futsal");
		TestUser player = this.users.signUp("Grumpy Player");
		String bookingId = played(player, venue, "07:00", 1);
		String reviewId = id(writeReview(player, bookingId, 2, "Lights were off"));
		String reviews = "/api/v1/owner/venues/" + venue.id() + "/reviews";

		send(owner, get(reviews).param("unanswered", "true"), null).andExpect(jsonPath("$.content[*].id", hasItem(reviewId)));
		send(this.users.signUp("Rival Owner"), put(reviews + "/" + reviewId + "/reply"), "{\"text\":\"Lies\"}")
			.andExpect(status().isNotFound());
		String receptionistPhone = TestUsers.nextLocalPhone();
		send(owner, post("/api/v1/owner/venues/" + venue.id() + "/staff"),
				"{\"phone\":\"%s\",\"fullName\":\"Desk\",\"role\":\"RECEPTIONIST\"}".formatted(receptionistPhone))
			.andExpect(status().isCreated());
		TestUser receptionist = this.users.logIn(receptionistPhone, "Desk");
		send(receptionist, get(reviews), null).andExpect(status().isOk());
		send(receptionist, put(reviews + "/" + reviewId + "/reply"), "{\"text\":\"Sorry\"}").andExpect(status().isForbidden());

		send(owner, put(reviews + "/" + reviewId + "/reply"), "{\"text\":\"Sorry, fixed the floodlights.\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ownerReply").value("Sorry, fixed the floodlights."));
		send(owner, get(reviews).param("unanswered", "true"), null)
			.andExpect(jsonPath("$.content[*].id", not(hasItem(reviewId))));
		send(player, get("/api/v1/notifications"), null)
			.andExpect(jsonPath("$.content[0].type").value("REVIEW_REPLY"))
			.andExpect(jsonPath("$.content[0].title").value("Reply Futsal replied to your review"));
		this.mvc.perform(get("/api/v1/venues/" + venue.slug() + "/reviews"))
			.andExpect(jsonPath("$.content[0].ownerReply").value("Sorry, fixed the floodlights."));

		send(owner, delete(reviews + "/" + reviewId + "/reply"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.ownerReply").value(nullValue()))
			.andExpect(jsonPath("$.ownerRepliedAt").value(nullValue()));
	}

	@Test
	void adminModerationControlsWhatCountsTowardsTheRating() throws Exception {
		TestUser owner = this.users.signUp("Moderated Owner");
		TestVenue venue = this.venues.active(owner, "Moderated Futsal");
		TestUser player = this.users.signUp("Spammer");
		String bookingId = played(player, venue, "07:00", 1);
		String reviewId = id(writeReview(player, bookingId, 1, "spam spam"));
		TestUser admin = this.users.signUpAdmin("Moderator");

		send(player, post("/api/v1/admin/reviews/" + reviewId + "/status"), "{\"status\":\"HIDDEN\"}")
			.andExpect(status().isForbidden());
		send(admin, post("/api/v1/admin/reviews/" + reviewId + "/status"), "{\"status\":\"HIDDEN\"}")
			.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("HIDDEN"));
		this.mvc.perform(get("/api/v1/venues/" + venue.slug()))
			.andExpect(jsonPath("$.averageRating").value(0.0))
			.andExpect(jsonPath("$.reviewCount").value(0));
		this.mvc.perform(get("/api/v1/venues/" + venue.slug() + "/reviews")).andExpect(jsonPath("$.totalElements").value(0));
		send(owner, get("/api/v1/owner/venues/" + venue.id() + "/reviews"), null)
			.andExpect(jsonPath("$.content[0].status").value("HIDDEN"));
		send(admin, get("/api/v1/admin/reviews").param("status", "HIDDEN"), null)
			.andExpect(jsonPath("$.content[*].id", hasItem(reviewId)));

		send(admin, post("/api/v1/admin/reviews/" + reviewId + "/status"), "{\"status\":\"REMOVED\"}")
			.andExpect(status().isOk());
		send(player, put("/api/v1/bookings/" + bookingId + "/review"), "{\"rating\":1}").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("REVIEW_REMOVED"));
		send(player, delete("/api/v1/bookings/" + bookingId + "/review"), null).andExpect(status().isConflict());
		send(owner, get("/api/v1/owner/venues/" + venue.id() + "/reviews"), null)
			.andExpect(jsonPath("$.totalElements").value(0));

		send(admin, post("/api/v1/admin/reviews/" + reviewId + "/status"), "{\"status\":\"PUBLISHED\"}")
			.andExpect(status().isOk());
		this.mvc.perform(get("/api/v1/venues/" + venue.slug())).andExpect(jsonPath("$.reviewCount").value(1));
	}

	/** A confirmed booking moved {@code daysAgo} days into the past and completed. */
	private String played(TestUser player, TestVenue venue, String time, int daysAgo) throws Exception {
		String bookingId = id(book(player, venue, time).andExpect(status().isCreated()));
		int shift = (int) (GAME_DAY.toEpochDay() - LocalDate.now(ZoneId.of("Asia/Kathmandu")).toEpochDay()) + daysAgo;
		this.jdbc.update("""
				UPDATE bookings SET status = 'COMPLETED', starts_at = starts_at - make_interval(days => ?),
				    ends_at = ends_at - make_interval(days => ?) WHERE id = ?::uuid""", shift, shift, bookingId);
		return bookingId;
	}

	private ResultActions book(TestUser player, TestVenue venue, String time) throws Exception {
		return send(player, post("/api/v1/bookings"),
				"{\"courtId\":\"%s\",\"startsAt\":\"%sT%s:00+05:45\"}".formatted(venue.courtId(), GAME_DAY, time));
	}

	private ResultActions writeReview(TestUser player, String bookingId, int rating, String comment) throws Exception {
		String json = (comment != null) ? "{\"rating\":%d,\"comment\":\"%s\"}".formatted(rating, comment)
				: "{\"rating\":%d}".formatted(rating);
		return send(player, post("/api/v1/bookings/" + bookingId + "/review"), json);
	}

	private static String id(ResultActions result) throws Exception {
		return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
	}

	private ResultActions send(TestUser user, MockHttpServletRequestBuilder request, String json) throws Exception {
		request.header("Authorization", user.bearer());
		if (json != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(json);
		}
		return this.mvc.perform(request);
	}

}
