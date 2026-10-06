package com.futslot.booking;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface BookingRepository extends JpaRepository<Booking, UUID> {

	boolean existsByReference(String reference);

	/** A player's bookings are those of every venue customer record linked to them, however they were made. */
	Optional<Booking> findByIdAndCustomerUserId(UUID id, UUID userId);

	Optional<Booking> findByIdAndVenueId(UUID id, UUID venueId);

	@Query("""
			select b from Booking b
			where b.customer.userId = :userId and b.endsAt > :now and b.status in :statuses
			order by b.startsAt""")
	List<Booking> findUpcomingForUser(UUID userId, Instant now, List<BookingStatus> statuses, Pageable pageable);

	@Query("""
			select b from Booking b
			where b.customer.userId = :userId and (b.endsAt <= :now or b.status not in :statuses)
			order by b.startsAt desc""")
	List<Booking> findPastForUser(UUID userId, Instant now, List<BookingStatus> statuses, Pageable pageable);

	@Query("""
			select b from Booking b
			where b.venue.id = :venueId and b.startsAt < :to and b.endsAt > :from
			order by b.startsAt, b.court.sortOrder""")
	List<Booking> findForVenueBetween(UUID venueId, Instant from, Instant to);

	@Query("""
			select count(b) from Booking b
			where b.bookedByUserId = :userId and b.status = com.futslot.booking.BookingStatus.PENDING_PAYMENT
			  and b.holdExpiresAt > :now""")
	long countActiveHolds(UUID userId, Instant now);

}
