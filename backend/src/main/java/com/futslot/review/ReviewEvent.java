package com.futslot.review;

import java.util.UUID;

/** Published after a review is written or answered; consumed by notifications after commit. */
public record ReviewEvent(Type type, UUID reviewId, UUID venueId, String venueName, UUID authorUserId,
		String authorName, int rating, String comment, UUID actorUserId) {

	public enum Type {

		/** A player reviewed the venue. */
		CREATED,
		/** The venue answered a review for the first time. */
		REPLIED

	}

	static ReviewEvent of(Type type, Review review, UUID actorUserId) {
		return new ReviewEvent(type, review.getId(), review.getVenueId(), review.getBooking().getVenue().getName(),
				review.getUser().getId(), ReviewResponse.shortName(review.getUser().getFullName()), review.getRating(),
				review.getComment(), actorUserId);
	}

}
