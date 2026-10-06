package com.futslot.venue;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface CourtRepository extends JpaRepository<Court, UUID> {

	boolean existsByVenueIdAndNameIgnoreCase(UUID venueId, String name);

	boolean existsByVenueIdAndNameIgnoreCaseAndIdNot(UUID venueId, String name, UUID id);

	Optional<Court> findByIdAndVenueId(UUID id, UUID venueId);

}
