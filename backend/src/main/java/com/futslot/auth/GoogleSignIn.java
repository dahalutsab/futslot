package com.futslot.auth;

import java.time.Clock;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.ApiException;
import com.futslot.common.PhoneNumbers;
import com.futslot.config.SecurityProperties;
import com.futslot.user.User;
import com.futslot.user.UserRepository;

/**
 * "Continue with Google". A Google account proves an email, not a phone, and FutSlot finds people by phone (staff,
 * invites, a venue's customers), so the first time the player also proves their number with a code. If the number
 * already has an account, Google is linked to it rather than making a second one.
 */
@Service
class GoogleSignIn {

	private final GoogleTokens google;

	private final AuthService auth;

	private final OtpService otpService;

	private final TokenService tokenService;

	private final UserRepository users;

	private final SecurityProperties.Google properties;

	private final Clock clock;

	GoogleSignIn(GoogleTokens google, AuthService auth, OtpService otpService, TokenService tokenService,
			UserRepository users, SecurityProperties properties, Clock clock) {
		this.google = google;
		this.auth = auth;
		this.otpService = otpService;
		this.tokenService = tokenService;
		this.users = users;
		this.properties = properties.google();
		this.clock = clock;
	}

	AuthOptions options() {
		return new AuthOptions(this.properties.enabled() ? this.properties.webClientId() : null);
	}

	/** Signs in an account Google is linked to; GOOGLE_PHONE_NEEDED says to link one with {@link #link}. */
	@Transactional
	TokenResponse signIn(GoogleSignInRequest request) {
		requireEnabled();
		GoogleTokens.GoogleIdentity identity = this.google.verify(request.idToken());
		User user = this.users.findByGoogleSubject(identity.subject())
			.orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "GOOGLE_PHONE_NEEDED",
					"Add your phone number to finish signing in"));
		AuthService.requireActive(user);
		user.recordLogin(this.clock.instant());
		return this.tokenService.issue(user);
	}

	@Transactional(noRollbackFor = ApiException.class)
	TokenResponse link(GoogleLinkRequest request) {
		requireEnabled();
		GoogleTokens.GoogleIdentity identity = this.google.verify(request.idToken());
		String phone = PhoneNumbers.normalize(request.phone());
		OtpCode otp = this.otpService.check(phone, OtpPurpose.VERIFY_PHONE, request.code());
		User linked = this.users.findByGoogleSubject(identity.subject()).orElse(null);
		if (linked != null && !linked.getPhone().equals(phone)) {
			throw new ApiException(HttpStatus.CONFLICT, "GOOGLE_ALREADY_LINKED",
					"This Google account is already linked to another number");
		}
		User user = this.users.findByPhone(phone)
			.orElseGet(() -> this.auth.register(phone, request.fullName() != null ? request.fullName() : identity.name()));
		AuthService.requireActive(user);
		if (user.getGoogleSubject() != null && !user.getGoogleSubject().equals(identity.subject())) {
			throw new ApiException(HttpStatus.CONFLICT, "GOOGLE_ALREADY_LINKED",
					"This number is linked to a different Google account");
		}
		user.linkGoogle(identity.subject());
		if (user.getEmail() == null && identity.email() != null && !this.users.existsByEmailIgnoreCase(identity.email())) {
			user.setEmail(identity.email());
		}
		Instant now = this.clock.instant();
		return this.auth.finish(user, otp, now);
	}

	private void requireEnabled() {
		if (!this.properties.enabled()) {
			throw new ApiException(HttpStatus.NOT_FOUND, "GOOGLE_SIGN_IN_OFF", "Google sign-in isn't available");
		}
	}

}
