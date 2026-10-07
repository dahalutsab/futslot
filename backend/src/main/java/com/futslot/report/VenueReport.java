package com.futslot.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A venue's numbers for a period. Bookings count by start time; money collected and refunds paid count by
 * when the money moved. Rates are percentages with one decimal, null when there's nothing to divide by.
 */
public record VenueReport(LocalDate from, LocalDate to, Bookings bookings, Money money, Occupancy occupancy,
		List<CourtRow> courts) {

	/** {@code played} = confirmed + completed + no-show: the bookings that took court time. */
	public record Bookings(long total, long played, long pendingPayment, long confirmed, long completed, long noShow,
			long cancelled, long expired, Map<String, Long> bySource, BigDecimal cancellationRate,
			BigDecimal noShowRate) {

	}

	/**
	 * {@code bookedValue} is the total of played bookings after discounts; {@code outstanding} is what those
	 * bookings still owe. {@code collected} and {@code refunded} are money in and out in the period, so
	 * {@code net = collected - refunded}. {@code refundsOwed} is everything waiting to be paid back now.
	 */
	public record Money(BigDecimal bookedValue, BigDecimal discounts, BigDecimal outstanding, BigDecimal collected,
			BigDecimal refunded, BigDecimal net, Map<String, BigDecimal> collectedByMethod,
			BigDecimal membershipSales, BigDecimal refundsOwed) {

	}

	/** Open hours count every active court for every day the venue is open in the period. */
	public record Occupancy(BigDecimal openHours, BigDecimal bookedHours, BigDecimal rate) {

	}

	public record CourtRow(UUID courtId, String name, boolean active, long bookings, BigDecimal bookedHours,
			BigDecimal bookedValue, BigDecimal occupancyRate) {

	}

}
