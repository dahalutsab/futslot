package com.futslot.auth;

import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Signing in with a texted code on pages FutSlot renders itself (a venue's website), where the page keeps its
 * own session instead of holding app tokens. Same codes, limits and account creation as the app.
 */
@Service
public class CodeSignIn {

	private final AuthService auth;

	CodeSignIn(AuthService auth) {
		this.auth = auth;
	}

	public void sendCode(String phone) {
		this.auth.requestOtp(new OtpRequest(phone));
	}

	/** The signed-in person's id. A new number needs {@code fullName} (FULL_NAME_REQUIRED otherwise). */
	public UUID verify(String phone, String code, String fullName) {
		return this.auth.signIn(phone, code, fullName).getId();
	}

}
