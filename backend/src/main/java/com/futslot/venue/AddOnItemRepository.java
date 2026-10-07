package com.futslot.venue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface AddOnItemRepository extends JpaRepository<AddOnItem, UUID> {

	List<AddOnItem> findByVenueIdOrderByCategoryAscNameAsc(UUID venueId);

	List<AddOnItem> findByVenueIdAndActiveTrueOrderByCategoryAscNameAsc(UUID venueId);

	Optional<AddOnItem> findByIdAndVenueId(UUID id, UUID venueId);

	boolean existsByVenueIdAndNameIgnoreCaseAndIdNot(UUID venueId, String name, UUID id);

}
