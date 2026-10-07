package com.futslot.billing;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * An admin defining a plan. {@code code} (e.g. PRO) is set once; leaving {@code yearlyPrice} empty means
 * the plan is monthly only; empty limits mean unlimited.
 */
public record PlanRequest(@Pattern(regexp = "[A-Z][A-Z0-9_]{1,29}") String code, @NotBlank @Size(max = 60) String name,
		@Size(max = 500) String description,
		@NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal monthlyPrice,
		@DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal yearlyPrice, @Min(1) Integer maxCourts,
		@Min(0) Integer maxStaff, @Size(max = 20) List<@Size(max = 40) String> features, Boolean active,
		Integer sortOrder) {

}
