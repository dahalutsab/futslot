package com.futslot.venue;

import java.util.List;
import java.util.UUID;

public record CourtResponse(UUID id, String name, CourtFormat format, CourtSurface surface, boolean indoor,
		int slotDurationMinutes, CourtStatus status, int sortOrder, List<PricingRuleResponse> pricingRules) {

	/** Owners see inactive rules too; the public only sees active ones. */
	static CourtResponse from(Court court, boolean includeInactiveRules) {
		List<PricingRuleResponse> rules = court.getPricingRules()
			.stream()
			.filter(rule -> includeInactiveRules || rule.isActive())
			.map(PricingRuleResponse::from)
			.toList();
		return new CourtResponse(court.getId(), court.getName(), court.getFormat(), court.getSurface(),
				court.isIndoor(), court.getSlotDurationMinutes(), court.getStatus(), court.getSortOrder(), rules);
	}

}
