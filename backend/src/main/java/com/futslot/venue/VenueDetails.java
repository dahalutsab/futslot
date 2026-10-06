package com.futslot.venue;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VenueDetails(@NotBlank @Size(max = 120) String name, @Size(max = 2000) String description,
		String phone, @Email @Size(max = 254) String email, @NotBlank @Size(max = 200) String addressLine,
		@Size(max = 80) String area, @NotBlank @Size(max = 80) String city,
		@DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
		@DecimalMin("-180") @DecimalMax("180") BigDecimal longitude) {

}
