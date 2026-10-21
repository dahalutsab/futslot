package com.futslot.auth;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.futslot.common.ApiException;

/**
 * What a password must be: 8 characters or more, no longer than bcrypt reads (72 bytes), not one of the most
 * common passwords, and not the phone number it signs in with.
 */
@Component
class PasswordPolicy {

	static final int MIN_LENGTH = 8;

	private static final int MAX_BYTES = 72;

	private final Set<String> common;

	PasswordPolicy() {
		try (var in = new ClassPathResource("common-passwords.txt").getInputStream()) {
			this.common = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
				.map(String::strip)
				.filter(line -> !line.isEmpty() && !line.startsWith("#"))
				.map(line -> line.toLowerCase(Locale.ROOT))
				.collect(Collectors.toUnmodifiableSet());
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/** {@code phone} is the E.164 number the password is for. */
	void check(String password, String phone) {
		if (password.codePointCount(0, password.length()) < MIN_LENGTH) {
			throw weak("Use at least " + MIN_LENGTH + " characters").variant("short").with(MIN_LENGTH);
		}
		if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
			throw weak("Use a shorter password").variant("long");
		}
		if (password.codePoints().distinct().count() == 1 || this.common.contains(password.toLowerCase(Locale.ROOT))) {
			throw weak("That password is too easy to guess").variant("common");
		}
		String digits = password.replaceAll("\\D", "");
		if (digits.length() >= MIN_LENGTH && phone.endsWith(digits)) {
			throw weak("Don't use your phone number as your password").variant("phone");
		}
	}

	private static ApiException weak(String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD", message);
	}

}
