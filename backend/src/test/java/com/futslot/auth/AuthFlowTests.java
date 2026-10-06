package com.futslot.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.futslot.support.IntegrationTest;
import com.futslot.support.RecordingSmsSender;
import com.futslot.support.TestUsers;
import com.jayway.jsonpath.JsonPath;

@IntegrationTest
class AuthFlowTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	RecordingSmsSender sms;

	@Test
	void newUserSignsUpWithNameAndCanReadProfile() throws Exception {
		String local = nextLocalPhone();
		String e164 = "+977" + local;
		requestOtp(local).andExpect(status().isAccepted()).andExpect(jsonPath("$.expiresInSeconds").value(300));
		String code = this.sms.lastCodeFor(e164);

		verify(local, code, null).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("FULL_NAME_REQUIRED"));

		String body = verify(local, code, "Sita Sharma").andExpect(status().isOk())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.user.phone").value(e164))
			.andExpect(jsonPath("$.user.roles[0]").value("PLAYER"))
			.andReturn().getResponse().getContentAsString();
		String accessToken = JsonPath.read(body, "$.accessToken");

		this.mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName").value("Sita Sharma"));
	}

	@Test
	void codeCannotBeUsedTwice() throws Exception {
		String phone = nextLocalPhone();
		requestOtp(phone);
		String code = this.sms.lastCodeFor("+977" + phone);

		verify(phone, code, "Ram Thapa").andExpect(status().isOk());
		verify(phone, code, "Ram Thapa").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("OTP_INVALID"));
	}

	@Test
	void codeIsLockedAfterTooManyWrongAttempts() throws Exception {
		String phone = nextLocalPhone();
		requestOtp(phone);
		String code = this.sms.lastCodeFor("+977" + phone);
		String wrong = code.equals("000000") ? "111111" : "000000";

		for (int i = 0; i < 5; i++) {
			verify(phone, wrong, "Hari").andExpect(jsonPath("$.code").value("OTP_INVALID"));
		}
		verify(phone, code, "Hari").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("OTP_INVALID"));
	}

	@Test
	void resendIsRateLimited() throws Exception {
		String phone = nextLocalPhone();
		requestOtp(phone).andExpect(status().isAccepted());
		requestOtp(phone).andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("OTP_COOLDOWN"));
	}

	@Test
	void refreshTokenRotatesAndReuseRevokesAllSessions() throws Exception {
		String first = login("Gita");
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
		String refreshToken = JsonPath.read(login("Maya"), "$.refreshToken");

		this.mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\"%s\"}".formatted(refreshToken))).andExpect(status().isNoContent());
		refresh(refreshToken).andExpect(status().isUnauthorized());
	}

	@Test
	void profileCanBeUpdated() throws Exception {
		String accessToken = JsonPath.read(login("Bikash"), "$.accessToken");

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
	}

	@Test
	void invalidInputIsRejected() throws Exception {
		requestOtp("12345").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PHONE"));
		verify("9800000000", "12ab", null).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors.code").exists());
		this.mvc.perform(post("/api/v1/auth/otp/request").contentType(MediaType.APPLICATION_JSON).content("{\"phone\":"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	private String login(String name) throws Exception {
		String phone = nextLocalPhone();
		requestOtp(phone);
		return verify(phone, this.sms.lastCodeFor("+977" + phone), name).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
	}

	private ResultActions requestOtp(String phone) throws Exception {
		return this.mvc.perform(post("/api/v1/auth/otp/request").contentType(MediaType.APPLICATION_JSON)
			.content("{\"phone\":\"%s\"}".formatted(phone)));
	}

	private ResultActions verify(String phone, String code, String fullName) throws Exception {
		String name = (fullName != null) ? ",\"fullName\":\"%s\"".formatted(fullName) : "";
		return this.mvc.perform(post("/api/v1/auth/otp/verify").contentType(MediaType.APPLICATION_JSON)
			.content("{\"phone\":\"%s\",\"code\":\"%s\"%s}".formatted(phone, code, name)));
	}

	private ResultActions refresh(String refreshToken) throws Exception {
		return this.mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)));
	}

	private static String nextLocalPhone() {
		return TestUsers.nextLocalPhone();
	}

}
