package com.futslot.auth;

/** Checks a Google ID token from "Sign in with Google" and says whose it is. */
public interface GoogleTokens {

	/** Throws {@code GOOGLE_TOKEN_INVALID} unless Google signed it for one of our client ids and it's current. */
	GoogleIdentity verify(String idToken);

	/** {@code subject} is Google's stable id for the account; {@code email} is set only when Google verified it. */
	record GoogleIdentity(String subject, String email, String name) {

	}

}
