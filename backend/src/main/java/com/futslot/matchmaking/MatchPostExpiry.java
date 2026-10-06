package com.futslot.matchmaking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Marks open posts whose start time has passed as EXPIRED, so listings stay clean. Reads already treat
 * them as expired. Filled posts keep their status: those matches happened.
 */
@Component
@ConditionalOnBooleanProperty(name = "futslot.matchmaking.expiry.enabled", matchIfMissing = true)
class MatchPostExpiry {

	private static final Logger logger = LoggerFactory.getLogger(MatchPostExpiry.class);

	private final JdbcClient jdbc;

	MatchPostExpiry(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Scheduled(fixedDelayString = "${futslot.matchmaking.expiry.interval-ms:600000}", initialDelay = 30_000)
	@Transactional
	void run() {
		int expired = this.jdbc
			.sql("UPDATE match_posts SET status = 'EXPIRED', updated_at = now() WHERE status = 'OPEN' AND starts_at <= now()")
			.update();
		if (expired > 0) {
			logger.info("Match posts expired: {}", expired);
		}
	}

}
