package com.futslot.review;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.futslot.booking.Booking;

interface ReviewRepository extends JpaRepository<Review, UUID> {

	boolean existsByBookingId(UUID bookingId);

	Optional<Review> findByBookingIdAndUserId(UUID bookingId, UUID userId);

	Optional<Review> findByIdAndVenueId(UUID id, UUID venueId);

	@EntityGraph(attributePaths = { "user", "booking", "booking.venue" })
	Page<Review> findByVenueIdAndStatus(UUID venueId, ReviewStatus status, Pageable pageable);

	@EntityGraph(attributePaths = { "user", "booking", "booking.venue" })
	Page<Review> findByVenueIdAndStatusIn(UUID venueId, Collection<ReviewStatus> statuses, Pageable pageable);

	@EntityGraph(attributePaths = { "user", "booking", "booking.venue" })
	Page<Review> findByVenueIdAndStatusInAndOwnerReplyIsNull(UUID venueId, Collection<ReviewStatus> statuses,
			Pageable pageable);

	@EntityGraph(attributePaths = { "user", "booking", "booking.venue" })
	Page<Review> findByUserId(UUID userId, Pageable pageable);

	@EntityGraph(attributePaths = { "user", "booking", "booking.venue" })
	Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

	@Query("""
			select r.rating as rating, count(r) as count from Review r
			where r.venueId = :venueId and r.status = com.futslot.review.ReviewStatus.PUBLISHED
			group by r.rating""")
	List<RatingCount> ratingCounts(UUID venueId);

	/** The player's completed bookings since {@code since} that they haven't reviewed, latest first. */
	@Query("""
			select b from Booking b
			where b.customer.userId = :userId and b.status = com.futslot.booking.BookingStatus.COMPLETED
			and b.endsAt >= :since and not exists (select 1 from Review r where r.booking = b)
			order by b.endsAt desc""")
	List<Booking> awaitingReview(UUID userId, Instant since);

	/**
	 * Serialises rating changes per venue. Taken before the review is written so that, under READ
	 * COMMITTED, {@link #refreshRating} runs after any competing change has committed and sees it.
	 */
	@Query(value = "SELECT id FROM venues WHERE id = :venueId FOR UPDATE", nativeQuery = true)
	UUID lockVenue(UUID venueId);

	/** Recomputes the venue's denormalised rating from its published reviews. */
	@Modifying(flushAutomatically = true)
	@Query(value = """
			UPDATE venues SET
			    average_rating = coalesce((SELECT round(avg(rating), 2) FROM reviews
			                               WHERE venue_id = :venueId AND status = 'PUBLISHED'), 0),
			    review_count = (SELECT count(*) FROM reviews WHERE venue_id = :venueId AND status = 'PUBLISHED')
			WHERE id = :venueId""", nativeQuery = true)
	void refreshRating(UUID venueId);

	interface RatingCount {

		Number getRating();

		Number getCount();

	}

}
