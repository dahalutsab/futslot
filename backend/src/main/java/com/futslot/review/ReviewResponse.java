package com.futslot.review;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import com.futslot.booking.Booking;

/**
 * {@code bookingId} is only filled in for the review's author, the venue and admins. The reviewer is
 * shown as first name and last initial ("Ram S.").
 */
public record ReviewResponse(UUID id, UUID venueId, String venueName, UUID bookingId, LocalDate playedOn,
		int rating, String comment, String reviewerName, String reviewerAvatarUrl, String ownerReply,
		Instant ownerRepliedAt, ReviewStatus status, Instant createdAt) {

	static ReviewResponse forPublic(Review review) {
		return from(review, false);
	}

	static ReviewResponse detailed(Review review) {
		return from(review, true);
	}

	private static ReviewResponse from(Review review, boolean detailed) {
		Booking booking = review.getBooking();
		LocalDate playedOn = booking.getStartsAt().atZone(ZoneId.of(booking.getVenue().getTimezone())).toLocalDate();
		return new ReviewResponse(review.getId(), review.getVenueId(), booking.getVenue().getName(),
				detailed ? booking.getId() : null, playedOn, review.getRating(), review.getComment(),
				shortName(review.getUser().getFullName()), review.getUser().getAvatarUrl(), review.getOwnerReply(),
				review.getOwnerRepliedAt(), review.getStatus(), review.getCreatedAt());
	}

	static String shortName(String fullName) {
		String[] parts = fullName.strip().split("\\s+");
		if (parts.length == 1) {
			return parts[0];
		}
		return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
	}

}
