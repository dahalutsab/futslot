package com.futslot.auth;

import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Signing in on pages FutSlot renders itself (a venue's website), where the page keeps its own session instead of
 * holding app tokens. A password, as in the app, with the same lock on wrong guesses; or a texted code, for someone
 * new (it makes their account) or who has no password or forgot it.
 */
@Service
public class PageSignIn {

	private final AuthService auth;

	PageSignIn(AuthService auth) {
		this.auth = auth;
	}

	/** The signed-in person's id. */
	public UUID withPassword(String phone, String password) {
		return this.auth.checkPassword(phone, password).getId();
	}

	/** The code to show on the page when codes are shown on screen (a demo without SMS), otherwise null. */
	public String sendCode(String phone) {
		return this.auth.requestOtp(new OtpRequest(phone)).code();
	}

	/** The signed-in person's id. A new number needs {@code fullName} (FULL_NAME_REQUIRED otherwise). */
	public UUID verify(String phone, String code, String fullName) {
		return this.auth.signInWithCode(phone, code, fullName).getId();
	}

}
