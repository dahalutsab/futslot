package com.futslot.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties("futslot.security")
public record SecurityProperties(@Valid Jwt jwt, @Valid Otp otp) {

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

}
