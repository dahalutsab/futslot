package com.futslot.venue;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** {@code dayOfWeek} null = every day. Windows can't cross midnight; split late-night pricing into two rules. */
public record PricingRuleRequest(PriceLabel label, DayOfWeek dayOfWeek, @NotNull LocalTime startTime,
		@NotNull LocalTime endTime, @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal price,
		Integer priority, LocalDate validFrom, LocalDate validTo, Boolean active) {

}
