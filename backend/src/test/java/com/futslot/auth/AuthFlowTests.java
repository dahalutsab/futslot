package com.futslot.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.futslot.support.FakeGoogleTokens;
import com.futslot.support.IntegrationTest;
import com.futslot.support.RecordingSmsSender;
import com.futslot.support.TestUsers;
import com.jayway.jsonpath.JsonPath;

@IntegrationTest
class AuthFlowTests {

	private static final String PASSWORD = "kick-off-at-seven";

	@Autowired
	MockMvc mvc;

	@Autowired
	RecordingSmsSender sms;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void newPlayerSignsUpWithACodeThenSignsInWithTheirPassword() throws Exception {
		String local = nextLocalPhone();
		String e164 = "+977" + local;
		requestCode(local, "SIGN_UP").andExpect(status().isAccepted()).andExpect(jsonPath("$.expiresInSeconds").value(300));
		String code = code(local);

		// A weak password leaves the code usable, so the app can ask again
		signUp(local, code, "Sita Sharma", "password123").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));
		String body = signUp(local, code, "Sita Sharma", PASSWORD).andExpect(status().isOk())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.user.phone").value(e164))
			.andExpect(jsonPath("$.user.roles[0]").value("PLAYER"))
			.andExpect(jsonPath("$.user.passwordSet").value(true))
			.andExpect(jsonPath("$.user.googleLinked").value(false))
			.andReturn().getResponse().getContentAsString();
		this.mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + JsonPath.read(body, "$.accessToken")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName").value("Sita Sharma"));

		signIn(local, PASSWORD).andExpect(status().isOk()).andExpect(jsonPath("$.user.fullName").value("Sita Sharma"));
		signIn("+977" + local, PASSWORD).andExpect(status().isOk());
		requestCode(local, "SIGN_UP").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ACCOUNT_EXISTS"));
	}

	@Test
	void codeCannotBeUsedTwice() throws Exception {
		String phone = nextLocalPhone();
		requestCode(phone, "SIGN_UP");
		String code = code(phone);

		signUp(phone, code, "Ram Thapa", PASSWORD).andExpect(status().isOk());
		signUp(phone, code, "Ram Thapa", PASSWORD).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("OTP_INVALID"));
	}

	@Test
	void codeIsLockedAfterTooManyWrongAttempts() throws Exception {
		String phone = nextLocalPhone();
		requestCode(phone, "SIGN_UP");
		String code = code(phone);
		String wrong = code.equals("000000") ? "111111" : "000000";

		for (int i = 0; i < 5; i++) {
			signUp(phone, wrong, "Hari", PASSWORD).andExpect(jsonPath("$.code").value("OTP_INVALID"));
		}
		signUp(phone, code, "Hari", PASSWORD).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("OTP_INVALID"));
	}

	@Test
	void resendIsRateLimited() throws Exception {
		String phone = nextLocalPhone();
		requestCode(phone, "SIGN_UP").andExpect(status().isAccepted());
		requestCode(phone, "SIGN_UP").andExpect(status().isTooManyRequests())
			.andExpect(jsonPath("$.code").value("OTP_COOLDOWN"));
	}

	@Test
	void wrongPasswordsLockSigningInUntilThePasswordIsReset() throws Exception {
		String phone = nextLocalPhone();
		signUpNew(phone, "Gopal");

		for (int i = 0; i < 4; i++) {
			signIn(phone, "not-my-password").andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
		}
		signIn(phone, "not-my-password").andExpect(status().isTooManyRequests())
			.andExpect(jsonPath("$.code").value("SIGN_IN_LOCKED"));
		signIn(phone, PASSWORD).andExpect(status().isTooManyRequests());

		requestCode(phone, "RESET_PASSWORD").andExpect(status().isAccepted());
		resetPassword(phone, code(phone), "a-brand-new-one").andExpect(status().isOk());
		signIn(phone, PASSWORD).andExpect(status().isUnauthorized());
		signIn(phone, "a-brand-new-one").andExpect(status().isOk());
	}

	@Test
	void anUnknownNumberLooksLikeAWrongPassword() throws Exception {
		signIn(nextLocalPhone(), PASSWORD).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
		requestCode(nextLocalPhone(), "RESET_PASSWORD").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
	}

	@Test
	void resettingThePasswordSignsOutOtherDevices() throws Exception {
		String phone = nextLocalPhone();
		String otherDevice = JsonPath.read(signUpNew(phone, "Kamala"), "$.refreshToken");

		requestCode(phone, "RESET_PASSWORD");
		String body = resetPassword(phone, code(phone), "a-brand-new-one").andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();

		refresh(otherDevice).andExpect(status().isUnauthorized());
		refresh(JsonPath.read(body, "$.refreshToken")).andExpect(status().isOk());
	}

	@Test
	void anAccountFromBeforePasswordsSetsOneWithACode() throws Exception {
		String phone = nextLocalPhone();
		this.jdbc.update("INSERT INTO users (full_name, phone) VALUES ('Old Timer', ?)", "+977" + phone);

		signIn(phone, PASSWORD).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PASSWORD_NOT_SET"));
		requestCode(phone, "RESET_PASSWORD").andExpect(status().isAccepted());
		resetPassword(phone, code(phone), PASSWORD).andExpect(status().isOk())
			.andExpect(jsonPath("$.user.fullName").value("Old Timer"))
			.andExpect(jsonPath("$.user.passwordSet").value(true));
		signIn(phone, PASSWORD).andExpect(status().isOk());
	}

	@Test
	void changingThePasswordNeedsTheCurrentOneAndSignsOutOtherDevices() throws Exception {
		String phone = nextLocalPhone();
		String first = signUpNew(phone, "Nabin");
		String accessToken = JsonPath.read(first, "$.accessToken");

		changePassword(accessToken, "wrong-one", "another-good-one").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("WRONG_PASSWORD"));
		String body = changePassword(accessToken, PASSWORD, "another-good-one").andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();

		refresh(JsonPath.read(first, "$.refreshToken")).andExpect(status().isUnauthorized());
		refresh(JsonPath.read(body, "$.refreshToken")).andExpect(status().isOk());
		signIn(phone, "another-good-one").andExpect(status().isOk());
	}

	@Test
	void passwordsThatAreEasyToGuessAreRefused() throws Exception {
		String phone = nextLocalPhone();
		requestCode(phone, "SIGN_UP");
		String code = code(phone);

		for (String weak : new String[] { "short1", "aaaaaaaaaa", "Football", "98" + phone.substring(2), phone }) {
			signUp(phone, code, "Weak Password", weak).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));
		}
		signUp(phone, code, "Weak Password", "न्यू रोड 2026").andExpect(status().isOk());
	}

	@Test
	void googleSignInLinksToTheNumberItIsProvenWith() throws Exception {
		this.mvc.perform(get("/api/v1/auth/options")).andExpect(status().isOk())
			.andExpect(jsonPath("$.googleWebClientId").value("test-client"));
		String token = FakeGoogleTokens.token("google-anita", "anita@example.com", "Anita Gurung");
		google(token).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("GOOGLE_PHONE_NEEDED"));

		String phone = nextLocalPhone();
		requestCode(phone, "LINK_GOOGLE").andExpect(status().isAccepted());
		linkGoogle(token, phone, code(phone)).andExpect(status().isOk())
			.andExpect(jsonPath("$.user.fullName").value("Anita Gurung"))
			.andExpect(jsonPath("$.user.email").value("anita@example.com"))
			.andExpect(jsonPath("$.user.passwordSet").value(false))
			.andExpect(jsonPath("$.user.googleLinked").value(true));

		google(token).andExpect(status().isOk()).andExpect(jsonPath("$.user.phone").value("+977" + phone));
		google("not-a-google-token").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("GOOGLE_TOKEN_INVALID"));
	}

	@Test
	void googleJoinsAnExistingAccountRatherThanMakingASecondOne() throws Exception {
		// An account a venue made when adding Prakash as staff
		String phone = nextLocalPhone();
		this.jdbc.update("INSERT INTO users (full_name, phone) VALUES ('Prakash Shrestha', ?)", "+977" + phone);
		String id = this.jdbc.queryForObject("SELECT id FROM users WHERE phone = ?", String.class, "+977" + phone);
		String token = FakeGoogleTokens.token("google-prakash", null, "P Shrestha");

		requestCode(phone, "LINK_GOOGLE");
		linkGoogle(token, phone, code(phone)).andExpect(status().isOk())
			.andExpect(jsonPath("$.user.id").value(id))
			.andExpect(jsonPath("$.user.fullName").value("Prakash Shrestha"))
			.andExpect(jsonPath("$.user.email").doesNotExist());
		google(token).andExpect(status().isOk()).andExpect(jsonPath("$.user.id").value(id));

		// Nor can the same Google account join a second number
		String another = nextLocalPhone();
		requestCode(another, "LINK_GOOGLE");
		linkGoogle(token, another, code(another)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("GOOGLE_ALREADY_LINKED"));
	}

	@Test
	void refreshTokenRotatesAndReuseRevokesAllSessions() throws Exception {
		String first = signUpNew(nextLocalPhone(), "Gita");
		String firstRefresh = JsonPath.read(first, "$.refreshToken");

		String second = refresh(firstRefresh).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		String secondRefresh = JsonPath.read(second, "$.refreshToken");
		assertThat(secondRefresh).isNotEqualTo(firstRefresh);

		// Replaying the rotated token looks like theft: it fails and kills the newer token too
		refresh(firstRefresh).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
		refresh(secondRefresh).andExpect(status().isUnauthorized());
	}

	@Test
	void logoutRevokesRefreshToken() throws Exception {
		String refreshToken = JsonPath.read(signUpNew(nextLocalPhone(), "Maya"), "$.refreshToken");

		this.mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\"%s\"}".formatted(refreshToken))).andExpect(status().isNoContent());
		refresh(refreshToken).andExpect(status().isUnauthorized());
	}

	@Test
	void profileCanBeUpdated() throws Exception {
		String accessToken = JsonPath.read(signUpNew(nextLocalPhone(), "Bikash"), "$.accessToken");

		this.mvc.perform(patch("/api/v1/me").header("Authorization", "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"fullName\":\"Bikash Rai\",\"email\":\"bikash@example.com\",\"preferredLanguage\":\"ne\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName").value("Bikash Rai"))
			.andExpect(jsonPath("$.email").value("bikash@example.com"))
			.andExpect(jsonPath("$.preferredLanguage").value("ne"));
	}

	@Test
	void protectedEndpointsRequireAToken() throws Exception {
		this.mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
		this.mvc.perform(get("/api/v1/me").header("Authorization", "Bearer not-a-jwt")).andExpect(status().isUnauthorized());
		this.mvc.perform(put("/api/v1/me/password").contentType(MediaType.APPLICATION_JSON)
			.content("{\"newPassword\":\"another-good-one\"}")).andExpect(status().isUnauthorized());
	}

	@Test
	void invalidInputIsRejected() throws Exception {
		requestCode("12345", "SIGN_UP").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PHONE"));
		signUp("9800000000", "12ab", "Someone", PASSWORD).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors.code").exists());
		this.mvc.perform(post("/api/v1/auth/codes").contentType(MediaType.APPLICATION_JSON).content("{\"phone\":"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	/** Signs up a new account with {@link #PASSWORD} and returns the token response. */
	private String signUpNew(String phone, String name) throws Exception {
		requestCode(phone, "SIGN_UP");
		return signUp(phone, code(phone), name, PASSWORD).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
	}

	private String code(String phone) {
		return this.sms.lastCodeFor("+977" + phone);
	}

	private ResultActions requestCode(String phone, String purpose) throws Exception {
		return send("/api/v1/auth/codes", "{\"phone\":\"%s\",\"purpose\":\"%s\"}".formatted(phone, purpose));
	}

	private ResultActions signUp(String phone, String code, String fullName, String password) throws Exception {
		return send("/api/v1/auth/sign-up", "{\"phone\":\"%s\",\"code\":\"%s\",\"fullName\":\"%s\",\"password\":\"%s\"}"
			.formatted(phone, code, fullName, password));
	}

	private ResultActions signIn(String phone, String password) throws Exception {
		return send("/api/v1/auth/sign-in", "{\"phone\":\"%s\",\"password\":\"%s\"}".formatted(phone, password));
	}

	private ResultActions resetPassword(String phone, String code, String password) throws Exception {
		return send("/api/v1/auth/password/reset",
				"{\"phone\":\"%s\",\"code\":\"%s\",\"password\":\"%s\"}".formatted(phone, code, password));
	}

	private ResultActions changePassword(String accessToken, String current, String next) throws Exception {
		return this.mvc.perform(put("/api/v1/me/password").header("Authorization", "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}".formatted(current, next)));
	}

	private ResultActions google(String idToken) throws Exception {
		return send("/api/v1/auth/google", "{\"idToken\":\"%s\"}".formatted(idToken));
	}

	private ResultActions linkGoogle(String idToken, String phone, String code) throws Exception {
		return send("/api/v1/auth/google/link",
				"{\"idToken\":\"%s\",\"phone\":\"%s\",\"code\":\"%s\"}".formatted(idToken, phone, code));
	}

	private ResultActions refresh(String refreshToken) throws Exception {
		return send("/api/v1/auth/refresh", "{\"refreshToken\":\"%s\"}".formatted(refreshToken));
	}

	private ResultActions send(String path, String json) throws Exception {
		return this.mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private static String nextLocalPhone() {
		return TestUsers.nextLocalPhone();
	}

}
