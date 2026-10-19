package com.futslot.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** A new password, with the code texted for RESET_PASSWORD. Also sets the first one for older accounts. */
record ResetPasswordRequest(@NotBlank String phone,
		@NotBlank @Pattern(regexp = "\\d{6}", message = "must be 6 digits") String code,
		@NotNull @Size(max = 200) String password) {

}
