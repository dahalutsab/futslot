package com.futslot.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.venue.Court;
import com.futslot.venue.OperatingHours;
import com.futslot.venue.Venue;
import com.futslot.venue.VenueAccess;
import com.futslot.venue.VenuePermission;

/**
 * Read-only numbers for venue owners and admins, aggregated in SQL. A booking belongs to the period by
 * its start time in the venue's time zone; money by when it was paid or refunded.
 */
@Service
class ReportService {

	/** Bookings that took (or will take) court time; the base for value, hours and occupancy. */
	static final String PLAYED = "('CONFIRMED', 'COMPLETED', 'NO_SHOW')";

	/** Payments where the money did arrive, even if some or all was later refunded. */
	private static final String RECEIVED = "('SUCCEEDED', 'PARTIALLY_REFUNDED', 'REFUNDED')";

	private static final ZoneId PLATFORM_ZONE = ZoneId.of("Asia/Kathmandu");

	private final JdbcClient jdbc;

	private final VenueAccess access;

	private final Clock clock;

	ReportService(JdbcClient jdbc, VenueAccess access, Clock clock) {
		this.jdbc = jdbc;
		this.access = access;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	VenueReport summary(UUID userId, UUID venueId, LocalDate from, LocalDate to) {
		Venue venue = this.access.require(userId, venueId, VenuePermission.VIEW_REPORTS);
		ReportPeriod period = period(venue, from, to);

		List<StatusRow> statuses = this.jdbc.sql("""
				SELECT status, source, count(*) AS bookings, coalesce(sum(total_amount), 0) AS value,
				       coalesce(sum(discount_amount), 0) AS discounts,
				       coalesce(sum(greatest(total_amount - paid_amount, 0)), 0) AS due,
				       coalesce(sum(extract(epoch FROM ends_at - starts_at)), 0) / 3600 AS hours
				FROM bookings
				WHERE venue_id = :venueId AND starts_at >= :start AND starts_at < :end
				GROUP BY status, source""")
			.params(params(venueId, period))
			.query((rs, n) -> new StatusRow(rs.getString("status"), rs.getString("source"), rs.getLong("bookings"),
					rs.getBigDecimal("value"), rs.getBigDecimal("discounts"), rs.getBigDecimal("due"),
					rs.getBigDecimal("hours")))
			.list();
		Map<String, Long> byStatus = statuses.stream()
			.collect(Collectors.groupingBy(StatusRow::status, Collectors.summingLong(StatusRow::bookings)));
		long played = count(byStatus, "CONFIRMED") + count(byStatus, "COMPLETED") + count(byStatus, "NO_SHOW");
		long expired = count(byStatus, "EXPIRED");
		long total = statuses.stream().mapToLong(StatusRow::bookings).sum();
		Map<String, Long> bySource = statuses.stream()
			.filter(StatusRow::played)
			.collect(Collectors.groupingBy(StatusRow::source, TreeMap::new, Collectors.summingLong(StatusRow::bookings)));
		VenueReport.Bookings bookings = new VenueReport.Bookings(total, played, count(byStatus, "PENDING_PAYMENT"),
				count(byStatus, "CONFIRMED"), count(byStatus, "COMPLETED"), count(byStatus, "NO_SHOW"),
				count(byStatus, "CANCELLED"), expired,
				bySource, percent(count(byStatus, "CANCELLED"), total - expired),
				percent(count(byStatus, "NO_SHOW"), count(byStatus, "COMPLETED") + count(byStatus, "NO_SHOW")));

		Map<String, BigDecimal> collectedByMethod = new TreeMap<>();
		BigDecimal membershipSales = BigDecimal.ZERO;
		for (MoneyRow row : this.jdbc.sql("""
				SELECT purpose, method, coalesce(sum(amount), 0) AS amount FROM payments
				WHERE venue_id = :venueId AND status IN %s AND paid_at >= :start AND paid_at < :end
				GROUP BY purpose, method""".formatted(RECEIVED))
			.params(params(venueId, period))
			.query((rs, n) -> new MoneyRow(rs.getString("purpose"), rs.getString("method"), rs.getBigDecimal("amount")))
			.list()) {
			collectedByMethod.merge(row.method(), row.amount(), BigDecimal::add);
			if ("MEMBERSHIP".equals(row.purpose())) {
				membershipSales = membershipSales.add(row.amount());
			}
		}
		BigDecimal collected = collectedByMethod.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal refunded = this.jdbc.sql("""
				SELECT coalesce(sum(r.amount), 0) FROM refunds r JOIN payments p ON p.id = r.payment_id
				WHERE p.venue_id = :venueId AND r.status = 'SUCCEEDED' AND r.processed_at >= :start AND r.processed_at < :end""")
			.params(params(venueId, period))
			.query(BigDecimal.class)
			.single();
		BigDecimal refundsOwed = this.jdbc.sql("""
				SELECT coalesce(sum(r.amount), 0) FROM refunds r JOIN payments p ON p.id = r.payment_id
				WHERE p.venue_id = :venueId AND r.status IN ('REQUESTED', 'PROCESSING')""")
			.param("venueId", venueId)
			.query(BigDecimal.class)
			.single();
		VenueReport.Money money = new VenueReport.Money(sumPlayed(statuses, StatusRow::value),
				sumPlayed(statuses, StatusRow::discounts), sumPlayed(statuses, StatusRow::due), collected, refunded,
				collected.subtract(refunded), collectedByMethod, membershipSales, refundsOwed);

		List<Court> courts = venue.getCourts();
		BigDecimal openHoursPerCourt = openHours(venue, period);
		BigDecimal bookedHours = sumPlayed(statuses, StatusRow::hours).setScale(1, RoundingMode.HALF_UP);
		long activeCourts = courts.stream().filter(Court::isActive).count();
		BigDecimal openHours = openHoursPerCourt.multiply(BigDecimal.valueOf(activeCourts));
		VenueReport.Occupancy occupancy = new VenueReport.Occupancy(openHours, bookedHours, ratio(bookedHours, openHours));

		Map<UUID, CourtTotals> perCourt = this.jdbc.sql("""
				SELECT court_id, count(*) AS bookings, coalesce(sum(total_amount), 0) AS value,
				       coalesce(sum(extract(epoch FROM ends_at - starts_at)), 0) / 3600 AS hours
				FROM bookings
				WHERE venue_id = :venueId AND starts_at >= :start AND starts_at < :end AND status IN %s
				GROUP BY court_id""".formatted(PLAYED))
			.params(params(venueId, period))
			.query((rs, n) -> new CourtTotals(rs.getObject("court_id", UUID.class), rs.getLong("bookings"),
					rs.getBigDecimal("value"), rs.getBigDecimal("hours")))
			.list()
			.stream()
			.collect(Collectors.toMap(CourtTotals::courtId, Function.identity()));
		List<VenueReport.CourtRow> courtRows = courts.stream()
			.filter(court -> court.isActive() || perCourt.containsKey(court.getId()))
			.map(court -> {
				CourtTotals totals = perCourt.getOrDefault(court.getId(),
						new CourtTotals(court.getId(), 0, BigDecimal.ZERO, BigDecimal.ZERO));
				BigDecimal hours = totals.hours().setScale(1, RoundingMode.HALF_UP);
				return new VenueReport.CourtRow(court.getId(), court.getName(), court.isActive(), totals.bookings(),
						hours, totals.value(), court.isActive() ? ratio(hours, openHoursPerCourt) : null);
			})
			.toList();
		return new VenueReport(period.from(), period.to(), bookings, money, occupancy, courtRows);
	}

	@Transactional(readOnly = true)
	List<DailyRow> daily(UUID userId, UUID venueId, LocalDate from, LocalDate to) {
		Venue venue = this.access.require(userId, venueId, VenuePermission.VIEW_REPORTS);
		ReportPeriod period = period(venue, from, to);
		Map<String, Object> params = params(venueId, period);
		params.put("tz", venue.getTimezone());
		Map<LocalDate, DailyRow> days = new HashMap<>();
		this.jdbc.sql("""
				SELECT (starts_at AT TIME ZONE :tz)::date AS day,
				       count(*) FILTER (WHERE status IN %1$s) AS bookings,
				       coalesce(sum(extract(epoch FROM ends_at - starts_at)) FILTER (WHERE status IN %1$s), 0) / 3600 AS hours,
				       coalesce(sum(total_amount) FILTER (WHERE status IN %1$s), 0) AS value,
				       count(*) FILTER (WHERE status = 'CANCELLED') AS cancelled
				FROM bookings
				WHERE venue_id = :venueId AND starts_at >= :start AND starts_at < :end
				GROUP BY day""".formatted(PLAYED))
			.params(params)
			.query((rs, n) -> new DailyRow(rs.getObject("day", LocalDate.class), rs.getLong("bookings"),
					rs.getBigDecimal("hours").setScale(1, RoundingMode.HALF_UP), rs.getBigDecimal("value"),
					rs.getLong("cancelled"), BigDecimal.ZERO))
			.list()
			.forEach(row -> days.put(row.date(), row));
		Map<LocalDate, BigDecimal> collected = new HashMap<>();
		this.jdbc.sql("""
				SELECT (paid_at AT TIME ZONE :tz)::date AS day, sum(amount) AS amount FROM payments
				WHERE venue_id = :venueId AND status IN %s AND paid_at >= :start AND paid_at < :end
				GROUP BY day""".formatted(RECEIVED))
			.params(params)
			.query((rs, n) -> Map.entry(rs.getObject("day", LocalDate.class), rs.getBigDecimal("amount")))
			.list()
			.forEach(entry -> collected.put(entry.getKey(), entry.getValue()));
		return period.from()
			.datesUntil(period.to().plusDays(1))
			.map(date -> {
				DailyRow row = days.get(date);
				BigDecimal money = collected.getOrDefault(date, BigDecimal.ZERO);
				return (row != null)
						? new DailyRow(date, row.bookings(), row.bookedHours(), row.bookedValue(), row.cancelled(), money)
						: new DailyRow(date, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0, money);
			})
			.toList();
	}

	@Transactional(readOnly = true)
	List<PeakHourCell> peakHours(UUID userId, UUID venueId, LocalDate from, LocalDate to) {
		Venue venue = this.access.require(userId, venueId, VenuePermission.VIEW_REPORTS);
		ReportPeriod period = period(venue, from, to);
		Map<String, Object> params = params(venueId, period);
		params.put("tz", venue.getTimezone());
		return this.jdbc.sql("""
				SELECT extract(isodow FROM starts_at AT TIME ZONE :tz)::int AS dow,
				       extract(hour FROM starts_at AT TIME ZONE :tz)::int AS hour, count(*) AS bookings
				FROM bookings
				WHERE venue_id = :venueId AND starts_at >= :start AND starts_at < :end AND status IN %s
				GROUP BY dow, hour ORDER BY dow, hour""".formatted(PLAYED))
			.params(params)
			.query((rs, n) -> new PeakHourCell(DayOfWeek.of(rs.getInt("dow")), rs.getInt("hour"), rs.getLong("bookings")))
			.list();
	}

	@Transactional(readOnly = true)
	List<TopCustomerRow> topCustomers(UUID userId, UUID venueId, LocalDate from, LocalDate to, int limit) {
		Venue venue = this.access.require(userId, venueId, VenuePermission.VIEW_REPORTS);
		ReportPeriod period = period(venue, from, to);
		Map<String, Object> params = params(venueId, period);
		params.put("limit", limit);
		return this.jdbc.sql("""
				SELECT c.id, c.name, c.phone, count(*) AS bookings, coalesce(sum(b.total_amount), 0) AS spent,
				       max(b.starts_at) AS last_played
				FROM bookings b JOIN venue_customers c ON c.id = b.customer_id
				WHERE b.venue_id = :venueId AND b.starts_at >= :start AND b.starts_at < :end AND b.status IN %s
				GROUP BY c.id, c.name, c.phone
				ORDER BY bookings DESC, spent DESC, c.name
				LIMIT :limit""".formatted(PLAYED))
			.params(params)
			.query((rs, n) -> new TopCustomerRow(rs.getObject("id", UUID.class), rs.getString("name"),
					rs.getString("phone"), rs.getLong("bookings"), rs.getBigDecimal("spent"),
					rs.getTimestamp("last_played").toInstant()))
			.list();
	}

	@Transactional(readOnly = true)
	PlatformReport platform(LocalDate from, LocalDate to) {
		ReportPeriod period = ReportPeriod.of(from, to, PLATFORM_ZONE, this.clock);
		Map<String, Object> range = Map.of("start", period.startParam(), "end", period.endParam());
		Map<String, Long> venuesByStatus = new TreeMap<>();
		this.jdbc.sql("SELECT status, count(*) AS venues FROM venues GROUP BY status")
			.query((rs, n) -> Map.entry(rs.getString("status"), rs.getLong("venues")))
			.list()
			.forEach(entry -> venuesByStatus.put(entry.getKey(), entry.getValue()));
		long newVenues = this.jdbc.sql("SELECT count(*) FROM venues WHERE created_at >= :start AND created_at < :end")
			.params(range)
			.query(Long.class)
			.single();
		long newUsers = this.jdbc.sql("SELECT count(*) FROM users WHERE created_at >= :start AND created_at < :end")
			.params(range)
			.query(Long.class)
			.single();
		Map<String, Object> totals = this.jdbc.sql("""
				SELECT count(*) AS bookings, count(*) FILTER (WHERE status IN %1$s) AS played,
				       coalesce(sum(total_amount) FILTER (WHERE status IN %1$s), 0) AS value,
				       count(DISTINCT venue_id) FILTER (WHERE status IN %1$s) AS venues
				FROM bookings WHERE starts_at >= :start AND starts_at < :end""".formatted(PLAYED))
			.params(range)
			.query()
			.singleRow();
		List<PlatformReport.VenueRow> topVenues = this.jdbc.sql("""
				SELECT v.id, v.name, v.city, count(*) AS bookings, coalesce(sum(b.total_amount), 0) AS value
				FROM bookings b JOIN venues v ON v.id = b.venue_id
				WHERE b.starts_at >= :start AND b.starts_at < :end AND b.status IN %s
				GROUP BY v.id, v.name, v.city
				ORDER BY bookings DESC, value DESC, v.name
				LIMIT 10""".formatted(PLAYED))
			.params(range)
			.query((rs, n) -> new PlatformReport.VenueRow(rs.getObject("id", UUID.class), rs.getString("name"),
					rs.getString("city"), rs.getLong("bookings"), rs.getBigDecimal("value")))
			.list();
		return new PlatformReport(period.from(), period.to(), venuesByStatus, newVenues, newUsers,
				((Number) totals.get("bookings")).longValue(), ((Number) totals.get("played")).longValue(),
				(BigDecimal) totals.get("value"), ((Number) totals.get("venues")).longValue(), topVenues);
	}

	ReportPeriod period(Venue venue, LocalDate from, LocalDate to) {
		return ReportPeriod.of(from, to, ZoneId.of(venue.getTimezone()), this.clock);
	}

	static Map<String, Object> params(UUID venueId, ReportPeriod period) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("venueId", venueId);
		params.put("start", period.startParam());
		params.put("end", period.endParam());
		return params;
	}

	/** Hours one court is open over the period, from the venue's weekly hours. */
	static BigDecimal openHours(Venue venue, ReportPeriod period) {
		Map<DayOfWeek, Long> minutesByDay = new EnumMap<>(DayOfWeek.class);
		for (OperatingHours hours : venue.getOperatingHours()) {
			minutesByDay.put(hours.dayOfWeek(), hours.isClosed() ? 0 : openMinutes(hours.openTime(), hours.closeTime()));
		}
		long minutes = period.from()
			.datesUntil(period.to().plusDays(1))
			.mapToLong(date -> minutesByDay.getOrDefault(date.getDayOfWeek(), 0L))
			.sum();
		return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
	}

	/** A close time at or before the open time means open past midnight. */
	static long openMinutes(LocalTime open, LocalTime close) {
		long minutes = Duration.between(open, close).toMinutes();
		return (minutes > 0) ? minutes : minutes + Duration.ofDays(1).toMinutes();
	}

	private static long count(Map<String, Long> byStatus, String status) {
		return byStatus.getOrDefault(status, 0L);
	}

	private static BigDecimal sumPlayed(List<StatusRow> rows, Function<StatusRow, BigDecimal> field) {
		return rows.stream().filter(StatusRow::played).map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static BigDecimal percent(long part, long whole) {
		return (whole > 0) ? ratio(BigDecimal.valueOf(part), BigDecimal.valueOf(whole)) : null;
	}

	private static BigDecimal ratio(BigDecimal part, BigDecimal whole) {
		return (whole.signum() > 0)
				? part.multiply(BigDecimal.valueOf(100)).divide(whole, 1, RoundingMode.HALF_UP) : null;
	}

	private record StatusRow(String status, String source, long bookings, BigDecimal value, BigDecimal discounts,
			BigDecimal due, BigDecimal hours) {

		boolean played() {
			return PLAYED.contains("'" + this.status + "'");
		}

	}

	private record MoneyRow(String purpose, String method, BigDecimal amount) {

	}

	private record CourtTotals(UUID courtId, long bookings, BigDecimal value, BigDecimal hours) {

	}

}
