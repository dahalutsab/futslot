package com.futslot.venue;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface CourtRepository extends JpaRepository<Court, UUID> {

	boolean existsByVenueIdAndNameIgnoreCase(UUID venueId, String name);

	boolean existsByVenueIdAndNameIgnoreCaseAndIdNot(UUID venueId, String name, UUID id);

	Optional<Court> findByIdAndVenueId(UUID id, UUID venueId);

	/**
	 * SELECT ... FOR UPDATE on the court row. Everything that changes what occupies a court (bookings,
	 * blocks) takes this lock first, so their overlap checks can't race each other.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from Court c where c.id = :id")
	Court lockById(UUID id);

}
