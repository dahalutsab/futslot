package com.futslot.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.ApiException;
import com.futslot.common.Hashing;
import com.futslot.common.Wording;
import com.futslot.common.Words;
import com.futslot.config.SecurityProperties;

@Service
class OtpService {

	private final SecureRandom random = new SecureRandom();

	private final OtpCodeRepository codes;

	private final SmsSender smsSender;

	private final SecurityProperties.Otp properties;

	private final Words words;

	private final Clock clock;

	OtpService(OtpCodeRepository codes, SmsSender smsSender, SecurityProperties properties, Words words, Clock clock) {
		this.codes = codes;
		this.smsSender = smsSender;
		this.properties = properties.otp();
		this.words = words;
		this.clock = clock;
	}

	/** Texts a new code. Returns it when codes are shown on screen (a demo without SMS), otherwise null. */
	@Transactional
	String send(String phone, OtpPurpose purpose) {
		Instant now = this.clock.instant();
		this.codes.findFirstByPhoneAndPurposeOrderByCreatedAtDesc(phone, purpose)
			.filter(latest -> latest.getCreatedAt().isAfter(now.minus(this.properties.resendCooldown())))
			.ifPresent(latest -> {
				throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_COOLDOWN",
						"Please wait before requesting another code");
			});
		if (this.codes.countByPhoneAndCreatedAtAfter(phone, now.minusSeconds(3600)) >= this.properties.maxPerHour()) {
			throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_RATE_LIMITED",
					"Too many codes requested. Try again later");
		}
		String code = "%06d".formatted(this.random.nextInt(1_000_000));
		this.codes.save(new OtpCode(phone, hash(phone, code), purpose, now, now.plus(this.properties.ttl())));
		// In the language of the screen asking for it; the code itself stays in Latin digits, to be typed
		Wording w = this.words.forRequest();
		this.smsSender.send(phone, w.get("sms.code", code, w.format().number(this.properties.ttl().toMinutes())));
		return this.properties.showCodes() ? code : null;
	}

	/**
	 * Checks the code against the latest one sent to the phone. Returns it unconsumed so the caller
	 * can consume it once the rest of the request succeeds. A wrong code counts as a failed attempt,
	 * so callers must not roll back on {@link ApiException}.
	 */
	OtpCode check(String phone, OtpPurpose purpose, String code) {
		Instant now = this.clock.instant();
		OtpCode otp = this.codes.findFirstByPhoneAndPurposeOrderByCreatedAtDesc(phone, purpose)
			.filter(latest -> latest.isUsable(now, this.properties.maxAttempts()))
			.orElseThrow(OtpService::invalid);
		if (!Hashing.matches(otp.getCodeHash(), hash(phone, code))) {
			otp.recordFailedAttempt();
			throw invalid();
		}
		return otp;
	}

	private String hash(String phone, String code) {
		return Hashing.hmacSha256Hex(this.properties.secret(), phone + ":" + code);
	}

	private static ApiException invalid() {
		return new ApiException(HttpStatus.BAD_REQUEST, "OTP_INVALID", "The code is incorrect or has expired");
	}

	long ttlSeconds() {
		return this.properties.ttl().toSeconds();
	}

	long resendCooldownSeconds() {
		return this.properties.resendCooldown().toSeconds();
	}

}
