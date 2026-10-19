package com.futslot.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** A new account: the code texted for SIGN_UP proves the number. */
record SignUpRequest(@NotBlank String phone,
		@NotBlank @Pattern(regexp = "\\d{6}", message = "must be 6 digits") String code,
		@NotBlank @Size(min = 2, max = 100) String fullName, @NotNull @Size(max = 200) String password) {

}
