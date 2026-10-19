package com.futslot.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.futslot.common.ApiException;
import com.futslot.common.Hashing;
import com.futslot.config.SecurityProperties;
import com.futslot.user.User;
import com.futslot.user.UserResponse;

/**
 * Issues short-lived JWT access tokens and long-lived opaque refresh tokens. Refresh tokens rotate on
 * every use; presenting an already-rotated token revokes every session of that user (token theft).
 */
@Service
class TokenService {

	private final SecureRandom random = new SecureRandom();

	private final JwtEncoder jwtEncoder;

	private final RefreshTokenRepository refreshTokens;

	private final SecurityProperties.Jwt properties;

	private final Clock clock;

	TokenService(JwtEncoder jwtEncoder, RefreshTokenRepository refreshTokens, SecurityProperties properties,
			Clock clock) {
		this.jwtEncoder = jwtEncoder;
		this.refreshTokens = refreshTokens;
		this.properties = properties.jwt();
		this.clock = clock;
	}

	TokenResponse issue(User user) {
		Instant now = this.clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(this.properties.issuer())
			.subject(user.getId().toString())
			.issuedAt(now)
			.expiresAt(now.plus(this.properties.accessTokenTtl()))
			.claim("roles", user.getRoles().stream().map(Enum::name).sorted().toList())
			.build();
		String accessToken = this.jwtEncoder
			.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();

		byte[] bytes = new byte[32];
		this.random.nextBytes(bytes);
		String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		this.refreshTokens.save(new RefreshToken(user, Hashing.sha256Hex(refreshToken), now,
				now.plus(this.properties.refreshTokenTtl())));

		return new TokenResponse(accessToken, "Bearer", this.properties.accessTokenTtl().toSeconds(), refreshToken,
				UserResponse.from(user));
	}

	/** Must run in a transaction that does not roll back on {@link ApiException}, so reuse revocation sticks. */
	TokenResponse rotate(String rawRefreshToken) {
		Instant now = this.clock.instant();
		RefreshToken token = this.refreshTokens.findByTokenHash(Hashing.sha256Hex(rawRefreshToken))
			.orElseThrow(TokenService::invalid);
		User user = token.getUser();
		if (token.isRevoked()) {
			this.refreshTokens.revokeAllForUser(user.getId(), now);
			throw invalid();
		}
		if (token.isExpired(now) || !user.isActive()) {
			throw invalid();
		}
		token.revoke(now);
		return issue(user);
	}

	void revoke(String rawRefreshToken) {
		this.refreshTokens.findByTokenHash(Hashing.sha256Hex(rawRefreshToken))
			.ifPresent(token -> token.revoke(this.clock.instant()));
	}

	/**
	 * Signs the user out everywhere, after a password change. The tokens are deleted rather than revoked: a device
	 * still holding one is simply signed out, instead of looking like a thief and ending the new session too.
	 */
	void signOutEverywhere(UUID userId) {
		this.refreshTokens.deleteAllForUser(userId);
	}

	private static ApiException invalid() {
		return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Session expired. Please log in again");
	}

}
