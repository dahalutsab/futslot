package com.futslot.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
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
			select b from Booking b
			where b.recurringBookingId = :seriesId and b.startsAt > :after and b.status in :statuses
			order by b.startsAt""")
	List<Booking> findOccurrencesAfter(UUID seriesId, Instant after, List<BookingStatus> statuses);

	List<Booking> findByCustomerIdOrderByStartsAtDesc(UUID customerId, Pageable pageable);

	/** By exact reference, part of the customer's name, or part of their phone number; newest first. */
	@Query("""
			select b from Booking b
			where b.venue.id = :venueId
			  and (upper(b.reference) = :reference or lower(b.customer.name) like :name escape '\\'
			       or b.customer.phone like :phone)
			order by b.startsAt desc""")
	List<Booking> search(UUID venueId, String reference, String name, String phone, Pageable pageable);

	@Query("""
			select b.status as status, count(b) as bookings, coalesce(sum(b.totalAmount), 0) as amount
			from Booking b where b.customer.id = :customerId group by b.status""")
	List<StatusTotal> totalsByStatus(UUID customerId);

	long countByCustomerIdAndStatusAndCancellationFeePercentGreaterThan(UUID customerId, BookingStatus status,
			int feePercent);

	/** Late cancellations (ones that cost a fee) and no-shows starting from {@code since}. */
	@Query("""
			select count(b) from Booking b where b.customer.id = :customerId and b.startsAt >= :since
			  and (b.status = com.futslot.booking.BookingStatus.NO_SHOW
			       or (b.status = com.futslot.booking.BookingStatus.CANCELLED and b.cancellationFeePercent > 0))""")
	long countStrikes(UUID customerId, Instant since);

	interface StatusTotal {

		BookingStatus getStatus();

		long getBookings();

		BigDecimal getAmount();

	}

	boolean existsByBookedByUserIdAndDepositBackedTrueAndStatusIn(UUID userId, Collection<BookingStatus> statuses);

	@Query("""
			select count(b) from Booking b
			where b.bookedByUserId = :userId and b.status = com.futslot.booking.BookingStatus.PENDING_PAYMENT
			  and (b.holdExpiresAt > :now or b.holdExpiresAt is null)""")
	long countActiveHolds(UUID userId, Instant now);

}
