package com.futslot.discount;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DiscountCodeResponse(UUID id, String code, String description, DiscountType type, BigDecimal value,
		BigDecimal maxDiscountAmount, BigDecimal minBookingAmount, Instant validFrom, Instant validTo,
		Integer usageLimit, Integer perUserLimit, int usedCount, boolean active) {

	static DiscountCodeResponse from(DiscountCode code) {
		return new DiscountCodeResponse(code.getId(), code.getCode(), code.getDescription(), code.getType(),
				code.getValue(), code.getMaxDiscountAmount(), code.getMinBookingAmount(), code.getValidFrom(),
				code.getValidTo(), code.getUsageLimit(), code.getPerUserLimit(), code.getUsedCount(), code.isActive());
	}

}
