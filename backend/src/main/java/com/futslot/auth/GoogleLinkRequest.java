package com.futslot.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Finishing a first Google sign-in: the phone, proven with the code texted for LINK_GOOGLE. {@code fullName} is
 * only needed for a new account when Google didn't give a name.
 */
record GoogleLinkRequest(@NotBlank String idToken, @NotBlank String phone,
		@NotBlank @Pattern(regexp = "\\d{6}", message = "must be 6 digits") String code,
		@Size(min = 2, max = 100) String fullName) {

}
