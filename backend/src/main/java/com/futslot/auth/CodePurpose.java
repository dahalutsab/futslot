package com.futslot.auth;

/** Why a code is texted: proving the number for a new account or a Google link, or resetting a password. */
enum CodePurpose {

	SIGN_UP, RESET_PASSWORD, LINK_GOOGLE;

	OtpPurpose otp() {
		return this == RESET_PASSWORD ? OtpPurpose.RESET_PASSWORD : OtpPurpose.VERIFY_PHONE;
	}

}
