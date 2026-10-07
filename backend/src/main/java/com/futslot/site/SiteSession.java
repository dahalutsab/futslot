package com.futslot.site;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.futslot.common.ApiException;
import com.futslot.user.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Who's signed in on a venue's website: a signed, HttpOnly cookie on that site's own address, holding the
 * player's id and when it stops being valid. SameSite=Lax keeps other sites from posting forms with it.
 */
@Component
class SiteSession {

	static final String COOKIE = "fs_player";

	private static final Duration LIFETIME = Duration.ofDays(30);

	private final byte[] secret;

	private final Clock clock;

	private final UserService users;

	SiteSession(SiteProperties properties, Clock clock, UserService users) {
		this.secret = properties.sessionSecret().getBytes(StandardCharsets.UTF_8);
		this.clock = clock;
		this.users = users;
	}

	/** The signed-in player, if the cookie is genuine, still valid and their account is still active. */
	Optional<UUID> player(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		return Arrays.stream(cookies)
			.filter(c -> COOKIE.equals(c.getName()))
			.findFirst()
			.flatMap(c -> read(c.getValue()))
			.filter(this::active);
	}

	private boolean active(UUID playerId) {
		try {
			return this.users.get(playerId).isActive();
		}
		catch (ApiException ex) {
			return false;
		}
	}

	void signIn(UUID playerId, HttpServletRequest request, HttpServletResponse response) {
		long expires = this.clock.instant().plus(LIFETIME).getEpochSecond();
		String payload = playerId + "." + expires;
		write(response, request, payload + "." + sign(payload), LIFETIME);
	}

	void signOut(HttpServletRequest request, HttpServletResponse response) {
		write(response, request, "", Duration.ZERO);
	}

	private Optional<UUID> read(String value) {
		String[] parts = value.split("\\.");
		if (parts.length != 3) {
			return Optional.empty();
		}
		String payload = parts[0] + "." + parts[1];
		if (!MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
			return Optional.empty();
		}
		try {
			if (Long.parseLong(parts[1]) < this.clock.instant().getEpochSecond()) {
				return Optional.empty();
			}
			return Optional.of(UUID.fromString(parts[0]));
		}
		catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

	private String sign(String payload) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(this.secret, "HmacSHA256"));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException | InvalidKeyException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static void write(HttpServletResponse response, HttpServletRequest request, String value, Duration maxAge) {
		ResponseCookie cookie = ResponseCookie.from(COOKIE, value)
			.httpOnly(true)
			.secure(request.isSecure())
			.sameSite("Lax")
			.path("/")
			.maxAge(maxAge)
			.build();
		response.addHeader("Set-Cookie", cookie.toString());
	}

}
