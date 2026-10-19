package com.futslot.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@SecurityRequirements // public: no bearer token needed
class AuthController {

	private final AuthService authService;

	private final GoogleSignIn googleSignIn;

	AuthController(AuthService authService, GoogleSignIn googleSignIn) {
		this.authService = authService;
		this.googleSignIn = googleSignIn;
	}

	/** What the sign-in screen offers. */
	@GetMapping("/options")
	AuthOptions options() {
		return this.googleSignIn.options();
	}

	/** Texts a code to prove the number: to sign up, reset a password or link a Google account. */
	@PostMapping("/codes")
	@ResponseStatus(HttpStatus.ACCEPTED)
	OtpSentResponse requestCode(@Valid @RequestBody CodeRequest request) {
		return this.authService.requestCode(request);
	}

	@PostMapping("/sign-up")
	TokenResponse signUp(@Valid @RequestBody SignUpRequest request) {
		return this.authService.signUp(request);
	}

	@PostMapping("/sign-in")
	TokenResponse signIn(@Valid @RequestBody SignInRequest request) {
		return this.authService.signIn(request);
	}

	/** Sets a new password with a RESET_PASSWORD code, signing out every other device. */
	@PostMapping("/password/reset")
	TokenResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		return this.authService.resetPassword(request);
	}

	/** Signs in with Google; GOOGLE_PHONE_NEEDED the first time, until {@code /google/link}. */
	@PostMapping("/google")
	TokenResponse google(@Valid @RequestBody GoogleSignInRequest request) {
		return this.googleSignIn.signIn(request);
	}

	/** A first Google sign-in, with the phone proven by a LINK_GOOGLE code. */
	@PostMapping("/google/link")
	TokenResponse linkGoogle(@Valid @RequestBody GoogleLinkRequest request) {
		return this.googleSignIn.link(request);
	}

	@PostMapping("/refresh")
	TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
		return this.authService.refresh(request);
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logout(@Valid @RequestBody RefreshRequest request) {
		this.authService.logout(request);
	}

}
