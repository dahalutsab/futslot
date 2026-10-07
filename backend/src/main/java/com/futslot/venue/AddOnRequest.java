package com.futslot.venue;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddOnRequest(@NotBlank @Size(max = 60) String name, @NotNull AddOnCategory category,
		@NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal price, Boolean active) {

}
