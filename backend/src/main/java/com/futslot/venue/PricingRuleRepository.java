package com.futslot.venue;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface PricingRuleRepository extends JpaRepository<PricingRule, UUID> {

	Optional<PricingRule> findByIdAndCourtId(UUID id, UUID courtId);

}
