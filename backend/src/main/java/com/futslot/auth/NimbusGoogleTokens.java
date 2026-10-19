package com.futslot.auth;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.futslot.common.ApiException;
import com.futslot.config.SecurityProperties;

/** Verifies ID tokens against Google's published signing keys, which the decoder fetches and caches. */
@Component
class NimbusGoogleTokens implements GoogleTokens {

	private static final String KEYS = "https://www.googleapis.com/oauth2/v3/certs";

	private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

	private final List<String> clientIds;

	private volatile JwtDecoder decoder;

	NimbusGoogleTokens(SecurityProperties properties) {
		this.clientIds = properties.google().audiences();
	}

	@Override
	public GoogleIdentity verify(String idToken) {
		try {
			Jwt jwt = decoder().decode(idToken);
			String email = Boolean.parseBoolean(String.valueOf(jwt.getClaims().get("email_verified")))
					? jwt.getClaimAsString("email") : null;
			return new GoogleIdentity(jwt.getSubject(), email, jwt.getClaimAsString("name"));
		}
		catch (JwtException ex) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "GOOGLE_TOKEN_INVALID",
					"Google sign-in didn't work. Try again");
		}
	}

	private JwtDecoder decoder() {
		JwtDecoder current = this.decoder;
		if (current == null) {
			NimbusJwtDecoder built = NimbusJwtDecoder.withJwkSetUri(KEYS).build();
			built.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(),
					new JwtClaimValidator<String>(JwtClaimNames.ISS, ISSUERS::contains),
					new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
							audience -> audience != null && audience.stream().anyMatch(this.clientIds::contains))));
			this.decoder = current = built;
		}
		return current;
	}

}
