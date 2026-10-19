package com.futslot.auth;

import jakarta.validation.constraints.NotBlank;

/** {@code idToken} is the credential "Sign in with Google" hands the app. */
record GoogleSignInRequest(@NotBlank String idToken) {

}
