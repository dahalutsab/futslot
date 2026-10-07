package com.futslot.booking;

import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Expires unpaid holds whose time is up and announces each one. */
@Component
class HoldExpiry {

	private final JdbcClient jdbc;

	private final BookingRepository bookings;

	private final ApplicationEventPublisher events;

	HoldExpiry(JdbcClient jdbc, BookingRepository bookings, ApplicationEventPublisher events) {
		this.jdbc = jdbc;
		this.bookings = bookings;
		this.events = events;
	}

	/** On one court ({@code courtId}) or everywhere (null). Must run inside a transaction. */
	int expire(UUID courtId, Instant now) {
		List<UUID> expired = this.jdbc.sql("""
				UPDATE bookings SET status = 'EXPIRED', updated_at = now(), version = version + 1
				WHERE status = 'PENDING_PAYMENT' AND hold_expires_at <= :now
				  AND (CAST(:courtId AS uuid) IS NULL OR court_id = :courtId)
				RETURNING id""")
			.param("now", now.atOffset(ZoneOffset.UTC))
			.param("courtId", courtId, Types.OTHER)
			.query(UUID.class)
			.list();
		if (!expired.isEmpty()) {
			this.bookings.findAllById(expired)
				.forEach(booking -> this.events.publishEvent(BookingEvent.of(BookingEventType.EXPIRED, booking, null, null)));
		}
		return expired.size();
	}

}
