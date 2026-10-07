package com.futslot.payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface RefundRepository extends JpaRepository<Refund, UUID> {

	@Query("select r from Refund r where r.id = :id and r.payment.venueId = :venueId")
	Optional<Refund> findForVenue(UUID id, UUID venueId);

	@Query("select r from Refund r where r.payment.venueId = :venueId and r.status = :status order by r.createdAt")
	List<Refund> findForVenue(UUID venueId, RefundStatus status);

	@Query("select r from Refund r where r.payment.booking.id = :bookingId order by r.createdAt")
	List<Refund> findForBooking(UUID bookingId);

}
