package com.futslot.membership;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Marks memberships past their last day (in the venue's time zone) as EXPIRED. Reads and bookings
 * already check the date, so this only keeps the stored status honest for listings.
 */
@Component
@ConditionalOnBooleanProperty(name = "futslot.memberships.expiry.enabled", matchIfMissing = true)
class MembershipExpiry {

	private static final Logger logger = LoggerFactory.getLogger(MembershipExpiry.class);

	private final JdbcClient jdbc;

	MembershipExpiry(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Scheduled(cron = "${futslot.memberships.expiry.cron:0 5 * * * *}")
	@Transactional
	void run() {
		int expired = this.jdbc.sql("""
				UPDATE memberships m SET status = 'EXPIRED', updated_at = now()
				FROM venues v
				WHERE v.id = m.venue_id AND m.status = 'ACTIVE'
				  AND m.expires_on < (now() AT TIME ZONE v.timezone)::date""").update();
		if (expired > 0) {
			logger.info("Memberships expired: {}", expired);
		}
	}

}
