package com.futslot.billing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {

	Optional<SubscriptionPlan> findByCode(String code);

	boolean existsByCode(String code);

	List<SubscriptionPlan> findAllByOrderBySortOrderAscMonthlyPriceAsc();

	List<SubscriptionPlan> findByActiveTrueOrderBySortOrderAscMonthlyPriceAsc();

}
