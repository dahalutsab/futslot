package com.futslot.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Development stand-in that writes SMS messages to the log. Replace with a real provider
 * (e.g. a Nepali SMS gateway) before launch; it must never be active in production.
 */
@Component
class LoggingSmsSender implements SmsSender {

	private static final Logger logger = LoggerFactory.getLogger(LoggingSmsSender.class);

	@Override
	public void send(String phone, String message) {
		logger.info("SMS to {}: {}", phone, message);
	}

}
