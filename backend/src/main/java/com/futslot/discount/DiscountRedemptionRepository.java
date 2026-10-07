package com.futslot.discount;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface DiscountRedemptionRepository extends JpaRepository<DiscountRedemption, UUID> {

	long countByDiscountCodeIdAndUserId(UUID discountCodeId, UUID userId);

	Optional<DiscountRedemption> findByBookingId(UUID bookingId);

}
