package com.futslot.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Partial update: null fields are left unchanged. */
public record UpdateProfileRequest(@Size(min = 2, max = 100) String fullName, @Email @Size(max = 254) String email,
		@Pattern(regexp = "en|ne", message = "must be 'en' or 'ne'") String preferredLanguage) {

}
