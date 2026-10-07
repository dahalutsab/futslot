package com.futslot.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.venue.Venue;
import com.futslot.venue.VenueAccess;
import com.futslot.venue.VenuePermission;

/**
 * Every booking in a period as CSV, for the venue's own accounts. Starts with a byte-order mark so Excel
 * reads Devanagari names correctly; times are in the venue's local time.
 */
@Component
class BookingExport {

	private static final Pattern NUMBER = Pattern.compile("[+-]?[0-9]+(\\.[0-9]+)?");

	private static final List<String> HEADER = List.of("Reference", "Date", "Start", "End", "Court", "Customer",
			"Phone", "Source", "Status", "Price", "Discount", "Total", "Paid", "Due", "Payment status", "Booked at");

	private final JdbcClient jdbc;

	private final VenueAccess access;

	private final ReportService reports;

	BookingExport(JdbcClient jdbc, VenueAccess access, ReportService reports) {
		this.jdbc = jdbc;
		this.access = access;
		this.reports = reports;
	}

	@Transactional(readOnly = true)
	Csv bookings(UUID userId, UUID venueId, LocalDate from, LocalDate to) {
		Venue venue = this.access.require(userId, venueId, VenuePermission.VIEW_REPORTS);
		ReportPeriod period = this.reports.period(venue, from, to);
		ZoneId zone = ZoneId.of(venue.getTimezone());
		StringBuilder csv = new StringBuilder("﻿");
		line(csv, HEADER);
		this.jdbc.sql("""
				SELECT b.reference, b.starts_at, b.ends_at, c.name AS court, vc.name AS customer, vc.phone, b.source,
				       b.status, b.price_amount, b.discount_amount, b.total_amount, b.paid_amount, b.payment_status,
				       b.created_at
				FROM bookings b JOIN courts c ON c.id = b.court_id JOIN venue_customers vc ON vc.id = b.customer_id
				WHERE b.venue_id = :venueId AND b.starts_at >= :start AND b.starts_at < :end
				ORDER BY b.starts_at, c.sort_order, c.name""")
			.params(ReportService.params(venueId, period))
			.query((rs, n) -> {
				OffsetDateTime start = rs.getObject("starts_at", OffsetDateTime.class).atZoneSameInstant(zone)
					.toOffsetDateTime();
				OffsetDateTime end = rs.getObject("ends_at", OffsetDateTime.class).atZoneSameInstant(zone)
					.toOffsetDateTime();
				BigDecimal total = rs.getBigDecimal("total_amount");
				BigDecimal paid = rs.getBigDecimal("paid_amount");
				return List.of(rs.getString("reference"), start.toLocalDate().toString(),
						start.toLocalTime().toString(), end.toLocalTime().toString(), rs.getString("court"),
						rs.getString("customer"), rs.getString("phone"), rs.getString("source"), rs.getString("status"),
						money(rs.getBigDecimal("price_amount")), money(rs.getBigDecimal("discount_amount")),
						money(total), money(paid), money(total.subtract(paid).max(BigDecimal.ZERO)),
						rs.getString("payment_status"),
						rs.getObject("created_at", OffsetDateTime.class).atZoneSameInstant(zone).toLocalDateTime()
							.withNano(0)
							.toString());
			})
			.list()
			.forEach(row -> line(csv, row));
		String filename = "%s-bookings-%s-to-%s.csv".formatted(venue.getSlug(), period.from(), period.to());
		return new Csv(filename, csv.toString());
	}

	private static String money(BigDecimal amount) {
		return amount.toPlainString();
	}

	private static void line(StringBuilder csv, List<String> cells) {
		for (int i = 0; i < cells.size(); i++) {
			if (i > 0) {
				csv.append(',');
			}
			csv.append(escape(cells.get(i)));
		}
		csv.append("\r\n");
	}

	/**
	 * Quotes cells that need it, and defuses ones a spreadsheet would run as a formula (customer names are
	 * typed by the public). Plain numbers and phone numbers such as +9779812345678 are left alone.
	 */
	static String escape(String value) {
		if (value == null) {
			return "";
		}
		boolean formula = !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0
				&& !NUMBER.matcher(value).matches();
		String cell = formula ? "'" + value : value;
		if (cell.contains(",") || cell.contains("\"") || cell.contains("\n") || cell.contains("\r")) {
			return "\"" + cell.replace("\"", "\"\"") + "\"";
		}
		return cell;
	}

	record Csv(String filename, String content) {

	}

}
