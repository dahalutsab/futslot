package com.futslot.booking;

import java.time.Clock;
import java.time.Instant;
import java.sql.Types;
import java.time.ZoneOffset;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Moves bookings along by time: lapsed holds expire, finished confirmed bookings complete. */
@Component
@ConditionalOnBooleanProperty(name = "futslot.bookings.maintenance.enabled", matchIfMissing = true)
class BookingMaintenance {

	private static final Logger logger = LoggerFactory.getLogger(BookingMaintenance.class);

	private final JdbcClient jdbc;

	private final Clock clock;

	BookingMaintenance(JdbcClient jdbc, Clock clock) {
		this.jdbc = jdbc;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${futslot.bookings.maintenance.interval-ms:60000}", initialDelay = 10_000)
	@Transactional
	void run() {
		Instant now = this.clock.instant();
		int expired = expireLapsedHolds(this.jdbc, null, now);
		int completed = this.jdbc.sql("""
				UPDATE bookings SET status = 'COMPLETED', updated_at = now(), version = version + 1
				WHERE status = 'CONFIRMED' AND ends_at <= :now""")
			.param("now", now.atOffset(ZoneOffset.UTC))
			.update();
		if (expired > 0 || completed > 0) {
			logger.info("Bookings maintenance: {} holds expired, {} bookings completed", expired, completed);
		}
	}

	/** Expires unpaid holds whose time is up, on one court ({@code courtId}) or everywhere (null). */
	static int expireLapsedHolds(JdbcClient jdbc, UUID courtId, Instant now) {
		return jdbc.sql("""
				UPDATE bookings SET status = 'EXPIRED', updated_at = now(), version = version + 1
				WHERE status = 'PENDING_PAYMENT' AND hold_expires_at <= :now
				  AND (CAST(:courtId AS uuid) IS NULL OR court_id = :courtId)""")
			.param("now", now.atOffset(ZoneOffset.UTC))
			.param("courtId", courtId, Types.OTHER)
			.update();
	}

}
