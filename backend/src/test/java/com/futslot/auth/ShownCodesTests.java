package com.futslot.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.futslot.support.IntegrationTest;
import com.futslot.support.RecordingSmsSender;
import com.futslot.support.TestUsers;
import com.jayway.jsonpath.JsonPath;

/** A demo without SMS shows the code on screen, and it works like a texted one. */
@IntegrationTest
@TestPropertySource(properties = "futslot.security.otp.show-codes=true")
class ShownCodesTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	RecordingSmsSender sms;

	@Test
	void theCodeComesBackToTheScreenAndSignsUp() throws Exception {
		String phone = TestUsers.nextLocalPhone();
		String body = this.mvc.perform(post("/api/v1/auth/codes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"phone\":\"%s\",\"purpose\":\"SIGN_UP\"}".formatted(phone)))
			.andExpect(status().isAccepted())
			.andReturn().getResponse().getContentAsString();
		String shown = JsonPath.read(body, "$.code");
		assertThat(shown).isEqualTo(this.sms.lastCodeFor("+977" + phone));

		this.mvc.perform(post("/api/v1/auth/sign-up").contentType(MediaType.APPLICATION_JSON)
				.content("{\"phone\":\"%s\",\"code\":\"%s\",\"fullName\":\"Demo Player\",\"password\":\"kick-off-at-seven\"}"
					.formatted(phone, shown)))
			.andExpect(status().isOk());
	}

}
