package com.futslot.review;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.booking.Booking;
import com.futslot.booking.BookingLookup;
import com.futslot.booking.BookingResponse;
import com.futslot.booking.BookingStatus;
import com.futslot.common.ApiException;
import com.futslot.common.PageResponse;
import com.futslot.user.User;
import com.futslot.user.UserRepository;
import com.futslot.venue.Venue;
import com.futslot.venue.VenueAccess;
import com.futslot.venue.VenuePermission;
import com.futslot.venue.VenueRepository;
import com.futslot.venue.VenueStatus;

/**
 * Players review a venue once per completed booking, within {@link #REVIEW_WINDOW} of playing, so every
 * review comes from someone who actually played there. Venues can reply but not hide reviews; only
 * admins moderate. The venue's average rating and count are recomputed on every change.
 */
@Service
public class ReviewService {

	static final Duration REVIEW_WINDOW = Duration.ofDays(30);

	/** What the venue sees: everything except reviews an admin removed. */
	private static final Set<ReviewStatus> VENUE_VISIBLE = EnumSet.of(ReviewStatus.PUBLISHED, ReviewStatus.HIDDEN);

	private final ReviewRepository reviews;

	private final BookingLookup bookings;

	private final UserRepository users;

	private final VenueRepository venues;

	private final VenueAccess access;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	ReviewService(ReviewRepository reviews, BookingLookup bookings, UserRepository users, VenueRepository venues,
			VenueAccess access, ApplicationEventPublisher events, Clock clock) {
		this.reviews = reviews;
		this.bookings = bookings;
		this.users = users;
		this.venues = venues;
		this.access = access;
		this.events = events;
		this.clock = clock;
	}

	// Player

	@Transactional
	ReviewResponse create(UUID userId, UUID bookingId, ReviewRequests.Write request) {
		Booking booking = this.bookings.forPlayer(userId, bookingId);
		if (booking.getStatus() != BookingStatus.COMPLETED) {
			throw new ApiException(HttpStatus.CONFLICT, "NOT_REVIEWABLE", "You can review a booking once you've played it");
		}
		if (booking.getEndsAt().plus(REVIEW_WINDOW).isBefore(this.clock.instant())) {
			throw new ApiException(HttpStatus.CONFLICT, "REVIEW_WINDOW_CLOSED",
					"Reviews can be left up to " + REVIEW_WINDOW.toDays() + " days after playing", REVIEW_WINDOW.toDays());
		}
		UUID venueId = booking.getVenue().getId();
		this.reviews.lockVenue(venueId);
		if (this.reviews.existsByBookingId(bookingId)) {
			throw new ApiException(HttpStatus.CONFLICT, "ALREADY_REVIEWED", "You've already reviewed this booking");
		}
		User author = this.users.findById(userId).orElseThrow();
		Review review = this.reviews.save(new Review(booking, author, request.rating(), text(request.comment())));
		this.reviews.refreshRating(venueId);
		this.events.publishEvent(ReviewEvent.of(ReviewEvent.Type.CREATED, review, userId));
		return ReviewResponse.detailed(review);
	}

	@Transactional
	ReviewResponse update(UUID userId, UUID bookingId, ReviewRequests.Write request) {
		Review review = own(userId, bookingId);
		if (review.getStatus() == ReviewStatus.REMOVED) {
			throw removed();
		}
		this.reviews.lockVenue(review.getVenueId());
		review.edit(request.rating(), text(request.comment()));
		this.reviews.refreshRating(review.getVenueId());
		return ReviewResponse.detailed(review);
	}

	/** A removed review stays, so it can't be deleted and posted again. */
	@Transactional
	void delete(UUID userId, UUID bookingId) {
		Review review = own(userId, bookingId);
		if (review.getStatus() == ReviewStatus.REMOVED) {
			throw removed();
		}
		this.reviews.lockVenue(review.getVenueId());
		this.reviews.delete(review);
		this.reviews.refreshRating(review.getVenueId());
	}

	@Transactional(readOnly = true)
	ReviewResponse forBooking(UUID userId, UUID bookingId) {
		return ReviewResponse.detailed(own(userId, bookingId));
	}

	@Transactional(readOnly = true)
	PageResponse<ReviewResponse> mine(UUID userId, Pageable pageable) {
		return PageResponse.of(this.reviews.findByUserId(userId, pageable), ReviewResponse::detailed);
	}

	/** Recently played bookings still open for a review, for a "How was your game?" prompt. */
	@Transactional(readOnly = true)
	List<BookingResponse> awaitingReview(UUID userId) {
		return this.reviews.awaitingReview(userId, this.clock.instant().minus(REVIEW_WINDOW))
			.stream()
			.map(BookingResponse::from)
			.toList();
	}

	// Public

	@Transactional(readOnly = true)
	public PageResponse<ReviewResponse> published(String slug, Pageable pageable) {
		Venue venue = activeVenue(slug);
		return PageResponse.of(this.reviews.findByVenueIdAndStatus(venue.getId(), ReviewStatus.PUBLISHED, pageable),
				ReviewResponse::forPublic);
	}

	@Transactional(readOnly = true)
	public RatingSummaryResponse summary(String slug) {
		Venue venue = activeVenue(slug);
		Map<Integer, Long> stars = new LinkedHashMap<>();
		for (int rating = 5; rating >= 1; rating--) {
			stars.put(rating, 0L);
		}
		this.reviews.ratingCounts(venue.getId())
			.forEach(count -> stars.put(count.getRating().intValue(), count.getCount().longValue()));
		return new RatingSummaryResponse(venue.getAverageRating(), venue.getReviewCount(), stars);
	}

	// Venue

	@Transactional(readOnly = true)
	PageResponse<ReviewResponse> forVenue(UUID userId, UUID venueId, boolean unansweredOnly, Pageable pageable) {
		this.access.require(userId, venueId, null);
		Page<Review> page = unansweredOnly
				? this.reviews.findByVenueIdAndStatusInAndOwnerReplyIsNull(venueId, VENUE_VISIBLE, pageable)
				: this.reviews.findByVenueIdAndStatusIn(venueId, VENUE_VISIBLE, pageable);
		return PageResponse.of(page, ReviewResponse::detailed);
	}

	/** Replying again replaces the reply; the author is only notified of the first one. */
	@Transactional
	ReviewResponse reply(UUID userId, UUID venueId, UUID reviewId, String text) {
		Review review = atVenue(userId, venueId, reviewId);
		boolean first = review.getOwnerReply() == null;
		review.reply(text.strip(), this.clock.instant());
		if (first) {
			this.events.publishEvent(ReviewEvent.of(ReviewEvent.Type.REPLIED, review, userId));
		}
		return ReviewResponse.detailed(review);
	}

	@Transactional
	ReviewResponse deleteReply(UUID userId, UUID venueId, UUID reviewId) {
		Review review = atVenue(userId, venueId, reviewId);
		review.reply(null, null);
		return ReviewResponse.detailed(review);
	}

	// Admin

	@Transactional(readOnly = true)
	PageResponse<ReviewResponse> byStatus(ReviewStatus status, Pageable pageable) {
		return PageResponse.of(this.reviews.findByStatus(status, pageable), ReviewResponse::detailed);
	}

	@Transactional
	ReviewResponse moderate(UUID reviewId, ReviewStatus status) {
		Review review = this.reviews.findById(reviewId).orElseThrow(ReviewService::notFound);
		this.reviews.lockVenue(review.getVenueId());
		review.moderate(status);
		this.reviews.refreshRating(review.getVenueId());
		return ReviewResponse.detailed(review);
	}

	private Review own(UUID userId, UUID bookingId) {
		return this.reviews.findByBookingIdAndUserId(bookingId, userId).orElseThrow(ReviewService::notFound);
	}

	private Review atVenue(UUID userId, UUID venueId, UUID reviewId) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_VENUE);
		Review review = this.reviews.findByIdAndVenueId(reviewId, venueId).orElseThrow(ReviewService::notFound);
		if (review.getStatus() == ReviewStatus.REMOVED) {
			throw removed();
		}
		return review;
	}

	private Venue activeVenue(String slug) {
		return this.venues.findBySlugAndStatus(slug, VenueStatus.ACTIVE)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VENUE_NOT_FOUND", "Venue not found"));
	}

	private static String text(String value) {
		return (value != null && !value.isBlank()) ? value.strip() : null;
	}

	private static ApiException notFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "Review not found");
	}

	private static ApiException removed() {
		return new ApiException(HttpStatus.CONFLICT, "REVIEW_REMOVED", "This review was removed by FutSlot");
	}

}
