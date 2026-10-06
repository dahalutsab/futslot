package com.futslot.discount;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * {@code code} and {@code type} are fixed once created. A PERCENT {@code value} is 1-100 and can be capped
 * with {@code maxDiscountAmount}; a FIXED one is rupees off.
 */
public record DiscountCodeRequest(@NotNull @Pattern(regexp = "\\s*[A-Za-z0-9_-]{3,32}\\s*",
		message = "use 3-32 letters, digits, - or _") String code, @Size(max = 200) String description,
		@NotNull DiscountType type, @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal value,
		@DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal maxDiscountAmount,
		@DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal minBookingAmount, OffsetDateTime validFrom,
		OffsetDateTime validTo, @Min(1) Integer usageLimit, @Min(1) Integer perUserLimit, Boolean active) {

}
