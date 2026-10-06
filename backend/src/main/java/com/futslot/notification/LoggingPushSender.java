package com.futslot.notification;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Development stand-in until Firebase is configured: logs pushes and reports them delivered. */
@Component
class LoggingPushSender implements PushSender {

	private static final Logger logger = LoggerFactory.getLogger(LoggingPushSender.class);

	@Override
	public Result send(List<String> tokens, String title, String body, Map<String, String> data) {
		logger.info("Push to {} device(s): {} - {} {}", tokens.size(), title, body, data);
		return new Result(true, Set.of());
	}

}
