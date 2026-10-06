package com.futslot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guards the rules the database itself enforces, so they keep holding no matter which code path writes.
 * Each test runs in a transaction that is rolled back afterwards.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class SchemaConstraintsTests {

	@Autowired
	JdbcTemplate jdbc;

	UUID ownerId;
	UUID venueId;
	UUID courtId;
	UUID customerId;

	@BeforeEach
	void createVenueWithCourt() {
		ownerId = insert("INSERT INTO users (full_name, phone) VALUES ('Owner', '+9779800000001') RETURNING id");
		venueId = insert("""
				INSERT INTO venues (owner_id, name, slug, address_line, city)
				VALUES (?, 'Test Futsal', 'test-futsal', 'Baneshwor', 'Kathmandu') RETURNING id""", ownerId);
		courtId = insert("INSERT INTO courts (venue_id, name) VALUES (?, 'Court A') RETURNING id", venueId);
		customerId = insert("""
				INSERT INTO venue_customers (venue_id, name, phone)
				VALUES (?, 'Walk-in', '+9779800000002') RETURNING id""", venueId);
	}

	@Test
	void backToBackBookingsAreAllowed() {
		book("FS-1", "2026-11-01 18:00+05:45", "2026-11-01 19:00+05:45", "CONFIRMED");
		book("FS-2", "2026-11-01 19:00+05:45", "2026-11-01 20:00+05:45", "CONFIRMED");

		assertThat(jdbc.queryForObject("SELECT count(*) FROM bookings", Integer.class)).isEqualTo(2);
	}

	@Test
	void overlappingBookingOnSameCourtIsRejected() {
		book("FS-1", "2026-11-01 18:00+05:45", "2026-11-01 19:00+05:45", "CONFIRMED");

		assertThatThrownBy(() -> book("FS-2", "2026-11-01 18:30+05:45", "2026-11-01 19:30+05:45", "PENDING_PAYMENT"))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("bookings_no_overlap");
	}

	@Test
	void cancelledBookingFreesTheSlot() {
		jdbc.update("""
				INSERT INTO bookings (reference, venue_id, court_id, customer_id, source, status, starts_at, ends_at,
				                      price_amount, total_amount, cancelled_at)
				VALUES ('FS-1', ?, ?, ?, 'PHONE', 'CANCELLED', '2026-11-01 18:00+05:45', '2026-11-01 19:00+05:45',
				        1500, 1500, now())""", venueId, courtId, customerId);

		book("FS-2", "2026-11-01 18:00+05:45", "2026-11-01 19:00+05:45", "CONFIRMED");

		assertThat(jdbc.queryForObject("SELECT count(*) FROM bookings", Integer.class)).isEqualTo(2);
	}

	@Test
	void sameTimeOnDifferentCourtsIsAllowed() {
		UUID courtB = insert("INSERT INTO courts (venue_id, name) VALUES (?, 'Court B') RETURNING id", venueId);

		book("FS-1", "2026-11-01 18:00+05:45", "2026-11-01 19:00+05:45", "CONFIRMED");
		jdbc.update("""
				INSERT INTO bookings (reference, venue_id, court_id, customer_id, source, status, starts_at, ends_at,
				                      price_amount, total_amount)
				VALUES ('FS-2', ?, ?, ?, 'WALK_IN', 'CONFIRMED', '2026-11-01 18:00+05:45', '2026-11-01 19:00+05:45',
				        1500, 1500)""", venueId, courtB, customerId);

		assertThat(jdbc.queryForObject("SELECT count(*) FROM bookings", Integer.class)).isEqualTo(2);
	}

	@Test
	void bookingTotalMustAddUp() {
		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO bookings (reference, venue_id, court_id, customer_id, source, starts_at, ends_at,
				                      price_amount, discount_amount, total_amount)
				VALUES ('FS-1', ?, ?, ?, 'APP', '2026-11-01 18:00+05:45', '2026-11-01 19:00+05:45', 1500, 200, 1500)""",
				venueId, courtId, customerId))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void overlappingCourtBlocksAreRejected() {
		String sql = """
				INSERT INTO court_blocks (court_id, starts_at, ends_at, reason, created_by)
				VALUES (?, ?::timestamptz, ?::timestamptz, 'MAINTENANCE', ?)""";
		jdbc.update(sql, courtId, "2026-11-02 06:00+05:45", "2026-11-02 09:00+05:45", ownerId);

		assertThatThrownBy(() -> jdbc.update(sql, courtId, "2026-11-02 08:00+05:45", "2026-11-02 10:00+05:45", ownerId))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void paymentMustTargetWhatItsPurposeSays() {
		UUID bookingId = book("FS-1", "2026-11-01 18:00+05:45", "2026-11-01 19:00+05:45", "CONFIRMED");
		jdbc.update("""
				INSERT INTO payments (purpose, booking_id, venue_id, amount, method, status, paid_at, recorded_by)
				VALUES ('BOOKING', ?, ?, 1500, 'CASH', 'SUCCEEDED', now(), ?)""", bookingId, venueId, ownerId);

		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO payments (purpose, booking_id, amount, method)
				VALUES ('MEMBERSHIP', ?, 1500, 'ESEWA')""", bookingId))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void freePlanIsSeeded() {
		assertThat(jdbc.queryForObject("SELECT monthly_price FROM subscription_plans WHERE code = 'FREE'", Integer.class))
			.isZero();
	}

	private UUID book(String reference, String startsAt, String endsAt, String status) {
		return insert("""
				INSERT INTO bookings (reference, venue_id, court_id, customer_id, source, status, starts_at, ends_at,
				                      price_amount, total_amount)
				VALUES (?, ?, ?, ?, 'APP', ?, ?::timestamptz, ?::timestamptz, 1500, 1500) RETURNING id""",
				reference, venueId, courtId, customerId, status, startsAt, endsAt);
	}

	private UUID insert(String sql, Object... args) {
		return jdbc.queryForObject(sql, UUID.class, args);
	}

}
