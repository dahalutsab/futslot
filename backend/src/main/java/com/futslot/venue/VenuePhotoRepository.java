package com.futslot.venue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface VenuePhotoRepository extends JpaRepository<VenuePhoto, UUID> {

	/** Cover first, then in the owner's order. */
	List<VenuePhoto> findByVenueIdOrderByIsCoverDescSortOrderAscCreatedAtAsc(UUID venueId);

	Optional<VenuePhoto> findByIdAndVenueId(UUID id, UUID venueId);

	Optional<VenuePhoto> findByVenueIdAndIsCoverTrue(UUID venueId);

	long countByVenueId(UUID venueId);

}
