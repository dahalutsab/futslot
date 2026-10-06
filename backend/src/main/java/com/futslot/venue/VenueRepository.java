package com.futslot.venue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VenueRepository extends JpaRepository<Venue, UUID>, JpaSpecificationExecutor<Venue> {

	boolean existsBySlug(String slug);

	List<Venue> findByOwnerIdOrderByCreatedAt(UUID ownerId);

	Optional<Venue> findByIdAndOwnerId(UUID id, UUID ownerId);

	Optional<Venue> findBySlugAndStatus(String slug, VenueStatus status);

	Page<Venue> findByStatus(VenueStatus status, Pageable pageable);

}
