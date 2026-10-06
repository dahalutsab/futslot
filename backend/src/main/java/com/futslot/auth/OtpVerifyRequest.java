package com.futslot.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** {@code fullName} is only required the first time a phone number logs in. */
record OtpVerifyRequest(@NotBlank String phone,
		@NotBlank @Pattern(regexp = "\\d{6}", message = "must be 6 digits") String code,
		@Size(min = 2, max = 100) String fullName) {

}
