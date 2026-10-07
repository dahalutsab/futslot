package com.futslot.auth;

import java.time.Clock;
import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.ApiException;
import com.futslot.common.PhoneNumbers;
import com.futslot.user.AccountCreated;
import com.futslot.user.User;
import com.futslot.user.UserRepository;

/**
 * Passwordless login: request a code by SMS, verify it, get tokens. The first successful
 * verification for a phone number creates the account, which needs a full name.
 */
@Service
class AuthService {

	private final OtpService otpService;

	private final TokenService tokenService;

	private final UserRepository users;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	AuthService(OtpService otpService, TokenService tokenService, UserRepository users, ApplicationEventPublisher events,
			Clock clock) {
		this.otpService = otpService;
		this.tokenService = tokenService;
		this.users = users;
		this.events = events;
		this.clock = clock;
	}

	OtpSentResponse requestOtp(OtpRequest request) {
		this.otpService.send(PhoneNumbers.normalize(request.phone()), OtpPurpose.LOGIN);
		return new OtpSentResponse(this.otpService.ttlSeconds(), this.otpService.resendCooldownSeconds());
	}

	@Transactional(noRollbackFor = ApiException.class)
	TokenResponse verifyOtp(OtpVerifyRequest request) {
		String phone = PhoneNumbers.normalize(request.phone());
		OtpCode otp = this.otpService.check(phone, OtpPurpose.LOGIN, request.code());
		Instant now = this.clock.instant();
		User user = this.users.findByPhone(phone).orElseGet(() -> register(phone, request.fullName()));
		if (!user.isActive()) {
			throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "This account has been disabled");
		}
		otp.consume(now);
		user.markPhoneVerified(now);
		user.recordLogin(now);
		return this.tokenService.issue(user);
	}

	@Transactional(noRollbackFor = ApiException.class)
	TokenResponse refresh(RefreshRequest request) {
		return this.tokenService.rotate(request.refreshToken());
	}

	@Transactional
	void logout(RefreshRequest request) {
		this.tokenService.revoke(request.refreshToken());
	}

	private User register(String phone, String fullName) {
		if (fullName == null || fullName.isBlank()) {
			// The code stays valid so the app can ask for a name and resubmit it
			throw new ApiException(HttpStatus.BAD_REQUEST, "FULL_NAME_REQUIRED", "Enter your name to create an account");
		}
		User user = this.users.save(new User(phone, fullName.strip()));
		this.events.publishEvent(new AccountCreated(user.getId(), user.getPhone()));
		return user;
	}

}
