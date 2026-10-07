package com.futslot.venue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface VenuePhotoRepository extends JpaRepository<VenuePhoto, UUID> {

	/** Cover first, then in the owner's order. */
	List<VenuePhoto> findByVenueIdOrderByIsCoverDescSortOrderAscCreatedAtAsc(UUID venueId);

	Optional<VenuePhoto> findByIdAndVenueId(UUID id, UUID venueId);

	Optional<VenuePhoto> findByVenueIdAndIsCoverTrue(UUID venueId);

	long countByVenueId(UUID venueId);

	/**
	 * Serialises photo changes per venue: the count, cover and order of a new photo depend on the photos
	 * already there, so two uploads at once must not both see none and both become the cover.
	 */
	@Query(value = "SELECT id FROM venues WHERE id = :venueId FOR UPDATE", nativeQuery = true)
	UUID lockVenue(UUID venueId);

}
