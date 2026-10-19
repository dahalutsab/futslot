package com.futslot.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties("futslot.security")
public record SecurityProperties(@Valid Jwt jwt, @Valid Otp otp, @Valid @DefaultValue Password password,
		@DefaultValue Google google) {

	public record Jwt(@NotBlank String secret, @DefaultValue("futslot") String issuer,
			@DefaultValue("15m") Duration accessTokenTtl, @DefaultValue("30d") Duration refreshTokenTtl) {

		@AssertTrue(message = "futslot.security.jwt.secret must be at least 32 bytes")
		boolean isSecretLongEnough() {
			return this.secret != null && this.secret.getBytes(StandardCharsets.UTF_8).length >= 32;
		}

	}

	public record Otp(@NotBlank String secret, @DefaultValue("5m") Duration ttl,
			@DefaultValue("5") @Min(1) int maxAttempts, @DefaultValue("60s") Duration resendCooldown,
			@DefaultValue("5") @Min(1) int maxPerHour) {

	}

	/** {@code maxFailures} wrong passwords in a row lock signing in to the account for {@code lockout}. */
	public record Password(@DefaultValue("5") @Min(1) int maxFailures, @DefaultValue("15m") Duration lockout) {

	}

	/**
	 * Google sign-in, off unless {@code webClientId} is set. The web apps and Android both sign in with the web client
	 * (Android names it as the server client), so their ID tokens are for it; {@code otherClientIds} adds the iOS
	 * client's.
	 */
	public record Google(String webClientId, @DefaultValue List<String> otherClientIds) {

		public boolean enabled() {
			return this.webClientId != null && !this.webClientId.isBlank();
		}

		/** Client ids an ID token may be for. */
		public List<String> audiences() {
			return enabled() ? Stream.concat(Stream.of(this.webClientId), this.otherClientIds.stream()).toList() : List.of();
		}

	}

}
