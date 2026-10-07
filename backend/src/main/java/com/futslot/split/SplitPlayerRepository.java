package com.futslot.split;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SplitPlayerRepository extends JpaRepository<SplitPlayer, UUID> {

	/** Booker first (added when the split is set up), then in the order they were added. */
	@EntityGraph(attributePaths = "user")
	List<SplitPlayer> findByBookingIdOrderByCreatedAtAscIdAsc(UUID bookingId);

	Optional<SplitPlayer> findByIdAndBookingId(UUID id, UUID bookingId);

	boolean existsByBookingIdAndUserId(UUID bookingId, UUID userId);

	boolean existsByBookingIdAndPhone(UUID bookingId, String phone);

	long countByBookingId(UUID bookingId);

	/** Shares the user still owes in other people's bookings. */
	@Query("""
			select p from SplitPlayer p
			where p.user.id = :userId and p.status = com.futslot.split.ParticipantStatus.ACCEPTED
			  and p.shareAmount > 0
			order by p.createdAt desc""")
	List<SplitPlayer> findOwedBy(UUID userId);

}
