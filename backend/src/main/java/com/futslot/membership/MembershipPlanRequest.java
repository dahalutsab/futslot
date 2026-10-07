package com.futslot.membership;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * GAME_PACK needs {@code gamesIncluded} and no {@code discountPercent}; MONTHLY_PASS needs
 * {@code discountPercent} and may cap the games with {@code gamesIncluded}.
 */
public record MembershipPlanRequest(@NotBlank @Size(max = 100) String name, @Size(max = 500) String description,
		@NotNull MembershipType type, @Min(1) @Max(500) Integer gamesIncluded, @Min(1) @Max(100) Integer discountPercent,
		@NotNull @Min(1) @Max(366) Integer validityDays,
		@NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal price, Boolean active) {

}
