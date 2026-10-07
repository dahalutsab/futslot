package com.futslot.venue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface VenueStaffRepository extends JpaRepository<VenueStaff, UUID> {

	Optional<VenueStaff> findByVenueIdAndUserId(UUID venueId, UUID userId);

	Optional<VenueStaff> findByIdAndVenueId(UUID id, UUID venueId);

	List<VenueStaff> findByVenueIdOrderByCreatedAt(UUID venueId);

	List<VenueStaff> findByVenueIdAndActiveTrue(UUID venueId);

	List<VenueStaff> findByUserIdAndActiveTrue(UUID userId);

}
