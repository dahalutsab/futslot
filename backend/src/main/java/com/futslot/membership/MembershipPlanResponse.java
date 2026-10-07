package com.futslot.membership;

import java.math.BigDecimal;
import java.util.UUID;

public record MembershipPlanResponse(UUID id, UUID venueId, String name, String description, MembershipType type,
		Integer gamesIncluded, Integer discountPercent, int validityDays, BigDecimal price, boolean active) {

	static MembershipPlanResponse from(MembershipPlan plan) {
		return new MembershipPlanResponse(plan.getId(), plan.getVenueId(), plan.getName(), plan.getDescription(),
				plan.getType(), plan.getGamesIncluded(), plan.getDiscountPercent(), plan.getValidityDays(),
				plan.getPrice(), plan.isActive());
	}

}
