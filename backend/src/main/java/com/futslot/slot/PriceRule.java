package com.futslot.slot;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.futslot.venue.PriceLabel;
import com.futslot.venue.PricingRule;

/** Plain copy of an active {@link PricingRule}, so slot calculation needs no persistence. */
public record PriceRule(UUID id, PriceLabel label, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
		BigDecimal price, int priority, LocalDate validFrom, LocalDate validTo) {

	public static PriceRule from(PricingRule rule) {
		return new PriceRule(rule.getId(), rule.getLabel(), rule.getDayOfWeek(), rule.getStartTime(), rule.getEndTime(),
				rule.getPrice(), rule.getPriority(), rule.getValidFrom(), rule.getValidTo());
	}

	boolean appliesTo(LocalDate operatingDate, LocalTime slotStart) {
		return (this.dayOfWeek == null || this.dayOfWeek == operatingDate.getDayOfWeek())
				&& (this.validFrom == null || !operatingDate.isBefore(this.validFrom))
				&& (this.validTo == null || !operatingDate.isAfter(this.validTo))
				&& !slotStart.isBefore(this.startTime) && slotStart.isBefore(this.endTime);
	}

}
