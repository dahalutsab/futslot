package com.futslot.membership;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

interface MembershipRepository extends JpaRepository<Membership, UUID> {

	boolean existsByPlanId(UUID planId);

	Optional<Membership> findByIdAndVenueId(UUID id, UUID venueId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select m from Membership m where m.id = :id")
	Optional<Membership> lockById(UUID id);

	@EntityGraph(attributePaths = { "plan", "venue", "customer" })
	Page<Membership> findByVenueId(UUID venueId, Pageable pageable);

	@EntityGraph(attributePaths = { "plan", "venue", "customer" })
	Page<Membership> findByVenueIdAndStatus(UUID venueId, MembershipStatus status, Pageable pageable);

	@EntityGraph(attributePaths = { "plan", "venue", "customer" })
	@Query("select m from Membership m where m.customer.userId = :userId order by m.createdAt desc")
	List<Membership> findForUser(UUID userId);

}
