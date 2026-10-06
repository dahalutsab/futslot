package com.futslot.common;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {

	private CurrentUser() {
	}

	/** The authenticated user's id, carried in the access token's {@code sub} claim. */
	public static UUID id(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

}
