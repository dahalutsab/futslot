package com.futslot.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/** Signs up fresh users through the real OTP flow and hands back their tokens. */
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
		String phone = nextLocalPhone();
		this.mvc.perform(post("/api/v1/auth/otp/request").contentType(MediaType.APPLICATION_JSON)
			.content("{\"phone\":\"%s\"}".formatted(phone))).andExpect(status().isAccepted());
		String code = this.sms.lastCodeFor("+977" + phone);
		String body = this.mvc
			.perform(post("/api/v1/auth/otp/verify").contentType(MediaType.APPLICATION_JSON)
				.content("{\"phone\":\"%s\",\"code\":\"%s\",\"fullName\":\"%s\"}".formatted(phone, code, fullName)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return toUser(body);
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
