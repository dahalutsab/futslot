package com.futslot.venue;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record PricingRuleResponse(UUID id, PriceLabel label, DayOfWeek dayOfWeek, LocalTime startTime,
		LocalTime endTime, BigDecimal price, int priority, LocalDate validFrom, LocalDate validTo, boolean active) {

	static PricingRuleResponse from(PricingRule rule) {
		return new PricingRuleResponse(rule.getId(), rule.getLabel(), rule.getDayOfWeek(), rule.getStartTime(),
				rule.getEndTime(), rule.getPrice(), rule.getPriority(), rule.getValidFrom(), rule.getValidTo(),
				rule.isActive());
	}

}
