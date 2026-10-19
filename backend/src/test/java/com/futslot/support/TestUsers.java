package com.futslot.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/** Signs up fresh users through the real sign-up flow (texted code, then a password) and hands back their tokens. */
public class TestUsers {

	private static final AtomicInteger phoneSequence = new AtomicInteger();

	private final MockMvc mvc;

	private final RecordingSmsSender sms;

	private final JdbcTemplate jdbc;

	TestUsers(MockMvc mvc, RecordingSmsSender sms, JdbcTemplate jdbc) {
		this.mvc = mvc;
		this.sms = sms;
		this.jdbc = jdbc;
	}

	/** A unique local Nepali mobile number (98XXXXXXXX) per call, so OTP cooldowns never collide. */
	public static String nextLocalPhone() {
		return "982" + "%07d".formatted(phoneSequence.incrementAndGet());
	}

	public TestUser signUp(String fullName) throws Exception {
		return logIn(nextLocalPhone(), fullName);
	}

	/** Every test account's password. */
	public static final String PASSWORD = "kick-off-at-seven";

	/**
	 * Signs in with a specific local phone number: signs up if it's new ({@code fullName} only matters then), or
	 * sets the password with a reset code if the number already has an account (made by a venue adding staff, say).
	 */
	public TestUser logIn(String phone, String fullName) throws Exception {
		int status = requestCode(phone, "SIGN_UP").andReturn().getResponse().getStatus();
		String body;
		if (status == 202) {
			body = send("/api/v1/auth/sign-up", "{\"phone\":\"%s\",\"code\":\"%s\",\"fullName\":\"%s\",\"password\":\"%s\"}"
				.formatted(phone, lastCode(phone), fullName, PASSWORD));
		}
		else {
			requestCode(phone, "RESET_PASSWORD").andExpect(status().isAccepted());
			body = send("/api/v1/auth/password/reset", "{\"phone\":\"%s\",\"code\":\"%s\",\"password\":\"%s\"}"
				.formatted(phone, lastCode(phone), PASSWORD));
		}
		return toUser(body);
	}

	private ResultActions requestCode(String phone, String purpose) throws Exception {
		return this.mvc.perform(post("/api/v1/auth/codes").contentType(MediaType.APPLICATION_JSON)
			.content("{\"phone\":\"%s\",\"purpose\":\"%s\"}".formatted(phone, purpose)));
	}

	private String send(String path, String json) throws Exception {
		return this.mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	private String lastCode(String phone) {
		return this.sms.lastCodeFor("+977" + phone);
	}

	public TestUser signUpAdmin(String fullName) throws Exception {
		TestUser user = signUp(fullName);
		this.jdbc.update("INSERT INTO user_roles (user_id, role) VALUES (?, 'ADMIN')", user.id());
		// A refreshed access token carries the new role
		String body = this.mvc
			.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"%s\"}".formatted(user.refreshToken())))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return toUser(body);
	}

	private static TestUser toUser(String body) {
		return new TestUser(UUID.fromString(JsonPath.read(body, "$.user.id")), JsonPath.read(body, "$.accessToken"),
				JsonPath.read(body, "$.refreshToken"));
	}

	public record TestUser(UUID id, String accessToken, String refreshToken) {

		public String bearer() {
			return "Bearer " + this.accessToken;
		}

	}

}
