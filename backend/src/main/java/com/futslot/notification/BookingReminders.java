package com.futslot.notification;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.Wording;
import com.futslot.common.Words;
import com.futslot.user.UserService;

/**
 * Reminds players shortly before their game. Claiming a booking (setting reminder_sent_at) and reading
 * its details happen in one UPDATE ... RETURNING, so even concurrent runs remind each booking once.
 */
@Component
@ConditionalOnBooleanProperty(name = "futslot.notifications.reminders.enabled", matchIfMissing = true)
class BookingReminders {

	private final JdbcClient jdbc;

	private final NotificationService notifications;

	private final UserService users;

	private final Words words;

	private final Duration lead;

	private final Clock clock;

	BookingReminders(JdbcClient jdbc, NotificationService notifications, UserService users, Words words,
			@Value("${futslot.notifications.reminders.lead:2h}") Duration lead, Clock clock) {
		this.jdbc = jdbc;
		this.notifications = notifications;
		this.users = users;
		this.words = words;
		this.lead = lead;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${futslot.notifications.reminders.interval-ms:300000}", initialDelay = 30_000)
	@Transactional
	int run() {
		Instant now = this.clock.instant();
		List<Due> due = this.jdbc.sql("""
				UPDATE bookings b SET reminder_sent_at = :now
				FROM venues v, courts c, venue_customers vc
				WHERE b.venue_id = v.id AND b.court_id = c.id AND b.customer_id = vc.id
				  AND b.status = 'CONFIRMED' AND b.reminder_sent_at IS NULL AND vc.user_id IS NOT NULL
				  AND b.starts_at > :now AND b.starts_at <= :until
				RETURNING b.id, b.reference, b.starts_at, v.id AS venue_id, v.name AS venue_name, v.timezone,
				          c.name AS court_name, vc.user_id""")
			.param("now", now.atOffset(ZoneOffset.UTC))
			.param("until", now.plus(this.lead).atOffset(ZoneOffset.UTC))
			.query((rs, row) -> new Due(rs.getObject("id", UUID.class), rs.getString("reference"),
					rs.getObject("starts_at", OffsetDateTime.class)
						.atZoneSameInstant(ZoneId.of(rs.getString("timezone")))
						.toOffsetDateTime(),
					rs.getObject("venue_id", UUID.class), rs.getString("venue_name"), rs.getString("court_name"),
					rs.getObject("user_id", UUID.class)))
			.list();
		for (Due booking : due) {
			Wording w = this.words.in(this.users.language(booking.userId()));
			this.notifications.send(booking.userId(), NotificationType.BOOKING_REMINDER, w.get("booking.reminder.title"),
					w.get("booking.reminder.body", booking.venueName(), booking.courtName(), Messages.time(booking.startsAt(), w),
							booking.reference()),
					Map.of("bookingId", booking.id().toString(), "venueId", booking.venueId().toString()));
		}
		return due.size();
	}

	private record Due(UUID id, String reference, OffsetDateTime startsAt, UUID venueId, String venueName,
			String courtName, UUID userId) {

	}

}
