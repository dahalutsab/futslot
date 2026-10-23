package com.futslot.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.ApiException;
import com.futslot.common.Languages;
import com.futslot.common.PhoneNumbers;
import com.futslot.config.SecurityProperties;
import com.futslot.user.AccountCreated;
import com.futslot.user.User;
import com.futslot.user.UserRepository;

/**
 * Signing in with a phone number and password. A texted code proves the number once, at sign-up, and again to
 * reset a forgotten password (which is also how accounts from before passwords set their first one). Wrong
 * passwords lock the account for a while. Venue websites still sign in with a code each time
 * ({@link PageSignIn}).
 */
@Service
class AuthService {

	private final OtpService otpService;

	private final TokenService tokenService;

	private final UserRepository users;

	private final PasswordEncoder passwords;

	private final PasswordPolicy policy;

	private final SecurityProperties.Password properties;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	/** Compared against when the phone has no account, so a wrong number takes as long as a wrong password. */
	private final String missingHash;

	AuthService(OtpService otpService, TokenService tokenService, UserRepository users, PasswordEncoder passwords,
			PasswordPolicy policy, SecurityProperties properties, ApplicationEventPublisher events, Clock clock) {
		this.otpService = otpService;
		this.tokenService = tokenService;
		this.users = users;
		this.passwords = passwords;
		this.policy = policy;
		this.properties = properties.password();
		this.events = events;
		this.clock = clock;
		this.missingHash = passwords.encode(UUID.randomUUID().toString());
	}

	/** Texts a code, after checking it can be used: sign-up needs a new number, a reset an existing account. */
	@Transactional
	OtpSentResponse requestCode(CodeRequest request) {
		String phone = PhoneNumbers.normalize(request.phone());
		boolean exists = this.users.findByPhone(phone).isPresent();
		if (request.purpose() == CodePurpose.SIGN_UP && exists) {
			throw accountExists();
		}
		if (request.purpose() == CodePurpose.RESET_PASSWORD && !exists) {
			throw new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
					"No account uses this number. Sign up instead");
		}
		return sent(this.otpService.send(phone, request.purpose().otp()));
	}

	@Transactional(noRollbackFor = ApiException.class)
	TokenResponse signUp(SignUpRequest request) {
		String phone = PhoneNumbers.normalize(request.phone());
		this.policy.check(request.password(), phone);
		OtpCode otp = this.otpService.check(phone, OtpPurpose.VERIFY_PHONE, request.code());
		if (this.users.findByPhone(phone).isPresent()) {
			throw accountExists();
		}
		Instant now = this.clock.instant();
		User user = register(phone, request.fullName());
		user.changePassword(this.passwords.encode(request.password()), now);
		return finish(user, otp, now);
	}

	@Transactional(noRollbackFor = ApiException.class)
	TokenResponse signIn(SignInRequest request) {
		return this.tokenService.issue(checkPassword(request.phone(), request.password()));
	}

	/** Wrong passwords count towards the lock, so this must not roll back on {@link ApiException}. */
	@Transactional(noRollbackFor = ApiException.class)
	User checkPassword(String rawPhone, String password) {
		String phone = PhoneNumbers.normalize(rawPhone);
		Instant now = this.clock.instant();
		User user = this.users.findByPhone(phone).orElse(null);
		if (user == null) {
			this.passwords.matches(password, this.missingHash);
			throw wrongPassword();
		}
		requireActive(user);
		if (user.isSignInLocked(now)) {
			throw locked();
		}
		if (!user.hasPassword()) {
			throw new ApiException(HttpStatus.CONFLICT, "PASSWORD_NOT_SET",
					"This account doesn't have a password yet. We'll text you a code to set one");
		}
		if (!this.passwords.matches(password, user.getPasswordHash())) {
			user.recordFailedSignIn(this.properties.maxFailures(), now.plus(this.properties.lockout()));
			throw user.isSignInLocked(now) ? locked() : wrongPassword();
		}
		user.recordLogin(now);
		return user;
	}

	/** A new password signs out every other device, in case someone else knew the old one. */
	@Transactional(noRollbackFor = ApiException.class)
	TokenResponse resetPassword(ResetPasswordRequest request) {
		String phone = PhoneNumbers.normalize(request.phone());
		this.policy.check(request.password(), phone);
		OtpCode otp = this.otpService.check(phone, OtpPurpose.RESET_PASSWORD, request.code());
		User user = this.users.findByPhone(phone).orElseThrow(AuthService::wrongPassword);
		requireActive(user);
		Instant now = this.clock.instant();
		user.changePassword(this.passwords.encode(request.password()), now);
		this.tokenService.signOutEverywhere(user.getId());
		return finish(user, otp, now);
	}

	/** Signs out every device and hands this one a new session. */
	@Transactional(noRollbackFor = ApiException.class)
	TokenResponse changePassword(UUID userId, ChangePasswordRequest request) {
		User user = this.users.findById(userId).orElseThrow(AuthService::wrongPassword);
		Instant now = this.clock.instant();
		if (user.hasPassword()) {
			if (user.isSignInLocked(now)) {
				throw locked();
			}
			if (request.currentPassword() == null
					|| !this.passwords.matches(request.currentPassword(), user.getPasswordHash())) {
				user.recordFailedSignIn(this.properties.maxFailures(), now.plus(this.properties.lockout()));
				throw new ApiException(HttpStatus.BAD_REQUEST, "WRONG_PASSWORD", "Your current password is wrong");
			}
		}
		this.policy.check(request.newPassword(), user.getPhone());
		user.changePassword(this.passwords.encode(request.newPassword()), now);
		this.tokenService.signOutEverywhere(user.getId());
		return this.tokenService.issue(user);
	}

	OtpSentResponse requestOtp(OtpRequest request) {
		return sent(this.otpService.send(PhoneNumbers.normalize(request.phone()), OtpPurpose.LOGIN));
	}

	/** Checks a LOGIN code and signs the person in, creating their account if it's their first time. */
	@Transactional(noRollbackFor = ApiException.class)
	User signInWithCode(String rawPhone, String code, String fullName) {
		String phone = PhoneNumbers.normalize(rawPhone);
		OtpCode otp = this.otpService.check(phone, OtpPurpose.LOGIN, code);
		Instant now = this.clock.instant();
		User user = this.users.findByPhone(phone).orElseGet(() -> register(phone, fullName));
		requireActive(user);
		otp.consume(now);
		user.markPhoneVerified(now);
		user.recordLogin(now);
		return user;
	}

	@Transactional(noRollbackFor = ApiException.class)
	TokenResponse refresh(RefreshRequest request) {
		return this.tokenService.rotate(request.refreshToken());
	}

	@Transactional
	void logout(RefreshRequest request) {
		this.tokenService.revoke(request.refreshToken());
	}

	/** Uses up the code that proved the phone and signs the person in. */
	TokenResponse finish(User user, OtpCode otp, Instant now) {
		otp.consume(now);
		user.markPhoneVerified(now);
		user.recordLogin(now);
		return this.tokenService.issue(user);
	}

	User register(String phone, String fullName) {
		if (fullName == null || fullName.isBlank()) {
			// The code stays valid so the app can ask for a name and resubmit it
			throw new ApiException(HttpStatus.BAD_REQUEST, "FULL_NAME_REQUIRED", "Enter your name to create an account");
		}
		User user = new User(phone, fullName.strip());
		// In the language they signed up in, so texts and notifications follow it from the start
		user.setPreferredLanguage(Languages.isNepali(LocaleContextHolder.getLocale()) ? "ne" : "en");
		user = this.users.save(user);
		this.events.publishEvent(new AccountCreated(user.getId(), user.getPhone()));
		return user;
	}

	static void requireActive(User user) {
		if (!user.isActive()) {
			throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "This account has been disabled");
		}
	}

	private OtpSentResponse sent(String code) {
		return new OtpSentResponse(this.otpService.ttlSeconds(), this.otpService.resendCooldownSeconds(), code);
	}

	private ApiException locked() {
		Duration lockout = this.properties.lockout();
		return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "SIGN_IN_LOCKED",
				"Too many wrong passwords. Try again in %d minutes, or reset your password"
					.formatted(lockout.toMinutes()), lockout.toMinutes());
	}

	private static ApiException accountExists() {
		return new ApiException(HttpStatus.CONFLICT, "ACCOUNT_EXISTS",
				"This number already has an account. Sign in, or reset your password");
	}

	private static ApiException wrongPassword() {
		return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Wrong phone number or password");
	}

}
