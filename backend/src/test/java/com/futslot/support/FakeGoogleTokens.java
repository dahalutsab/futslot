package com.futslot.support;

import org.springframework.http.HttpStatus;

import com.futslot.auth.GoogleTokens;
import com.futslot.common.ApiException;

/** Reads made-up ID tokens instead of asking Google: {@code google|subject|email|name}, made by {@link #token}. */
public class FakeGoogleTokens implements GoogleTokens {

	public static String token(String subject, String email, String name) {
		return String.join("|", "google", subject, email == null ? "" : email, name == null ? "" : name);
	}

	@Override
	public GoogleIdentity verify(String idToken) {
		String[] parts = idToken.split("\\|", -1);
		if (parts.length != 4 || !parts[0].equals("google")) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "GOOGLE_TOKEN_INVALID", "Google sign-in didn't work");
		}
		return new GoogleIdentity(parts[1], parts[2].isEmpty() ? null : parts[2], parts[3].isEmpty() ? null : parts[3]);
	}

}
