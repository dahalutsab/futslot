package com.futslot.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
@Import({ TestUsers.class, TestVenues.class })
public class TestSupportConfig {

	@Bean
	@Primary
	RecordingSmsSender recordingSmsSender() {
		return new RecordingSmsSender();
	}

	@Bean
	@Primary
	FakeGoogleTokens fakeGoogleTokens() {
		return new FakeGoogleTokens();
	}

	@Bean
	@Primary
	RecordingPushSender recordingPushSender() {
		return new RecordingPushSender();
	}

}
