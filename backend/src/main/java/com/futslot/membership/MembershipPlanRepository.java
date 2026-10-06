package com.futslot.membership;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface MembershipPlanRepository extends JpaRepository<MembershipPlan, UUID> {

	List<MembershipPlan> findByVenueIdOrderByCreatedAt(UUID venueId);

	List<MembershipPlan> findByVenueIdAndActiveTrueOrderByPrice(UUID venueId);

	Optional<MembershipPlan> findByIdAndVenueId(UUID id, UUID venueId);

}
