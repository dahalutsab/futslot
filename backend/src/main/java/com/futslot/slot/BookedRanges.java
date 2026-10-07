package com.futslot.slot;

import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Time occupied by live bookings. Unpaid holds whose hold has lapsed count as free: the booking
 * module expires them before inserting, so they never block a new booking. A hold with no expiry
 * (payment submitted, awaiting verification) still occupies its slot.
 */
@Component
public class BookedRanges {

	private final JdbcClient jdbc;

	BookedRanges(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public Map<UUID, List<TimeRange>> byCourt(Collection<UUID> courtIds, Instant from, Instant to, Instant now) {
		return byCourt(courtIds, from, to, now, null);
	}

	/** As {@link #byCourt(Collection, Instant, Instant, Instant)}, ignoring one booking (the one being moved). */
	public Map<UUID, List<TimeRange>> byCourt(Collection<UUID> courtIds, Instant from, Instant to, Instant now,
			UUID excludeBookingId) {
		Map<UUID, List<TimeRange>> result = new HashMap<>();
		if (courtIds.isEmpty()) {
			return result;
		}
		this.jdbc.sql("""
				SELECT court_id, starts_at, ends_at FROM bookings
				WHERE court_id IN (:courtIds) AND starts_at < :to AND ends_at > :from
				  AND status NOT IN ('CANCELLED', 'EXPIRED')
				  AND NOT (status = 'PENDING_PAYMENT' AND hold_expires_at IS NOT NULL AND hold_expires_at <= :now)
				  AND id IS DISTINCT FROM :excludeId""")
			.param("courtIds", courtIds)
			.param("from", utc(from))
			.param("to", utc(to))
			.param("now", utc(now))
			.param("excludeId", excludeBookingId, Types.OTHER)
			.query((rs, row) -> {
				UUID courtId = rs.getObject("court_id", UUID.class);
				TimeRange range = new TimeRange(rs.getObject("starts_at", OffsetDateTime.class).toInstant(),
						rs.getObject("ends_at", OffsetDateTime.class).toInstant());
				result.computeIfAbsent(courtId, id -> new ArrayList<>()).add(range);
				return null;
			})
			.list();
		return result;
	}

	private static OffsetDateTime utc(Instant instant) {
		return instant.atOffset(ZoneOffset.UTC);
	}

}
