package com.futslot.slot;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.futslot.venue.OperatingHours;
import com.futslot.venue.PriceLabel;

class SlotCalculatorTests {

	private static final ZoneId KATHMANDU = ZoneId.of("Asia/Kathmandu");

	// A Saturday
	private static final LocalDate DATE = LocalDate.of(2026, 11, 7);

	private static final Instant LONG_AGO = Instant.parse("2020-01-01T00:00:00Z");

	@Test
	void hourlySlotsFromOpenToClose() {
		List<Slot> slots = slots(hours("06:00", "22:00"), 60, List.of(rule(null, "00:00", "23:59", 1200, 0)));

		assertThat(slots).hasSize(16);
		assertThat(slots.getFirst().startsAt()).isEqualTo(OffsetDateTime.parse("2026-11-07T06:00+05:45"));
		assertThat(slots.getLast().endsAt()).isEqualTo(OffsetDateTime.parse("2026-11-07T22:00+05:45"));
		assertThat(slots).allMatch(slot -> slot.status() == SlotStatus.AVAILABLE);
	}

	@Test
	void dayRunningPastMidnightKeepsLateSlotsOnTheSameOperatingDay() {
		List<Slot> slots = slots(hours("18:00", "01:00"), 60,
				List.of(rule(null, "18:00", "23:59", 1800, 0), rule(null, "00:00", "02:00", 1500, 0)));

		assertThat(slots).hasSize(7);
		assertThat(slots.getLast().startsAt()).isEqualTo(OffsetDateTime.parse("2026-11-08T00:00+05:45"));
		assertThat(slots.getLast().price()).isEqualByComparingTo("1500");
	}

	@Test
	void trailingTimeShorterThanASlotIsNotOffered() {
		assertThat(slots(hours("06:00", "09:30"), 60, List.of(rule(null, "00:00", "23:59", 1000, 0)))).hasSize(3);
		assertThat(slots(hours("06:00", "09:30"), 90, List.of(rule(null, "00:00", "23:59", 1000, 0)))).hasSize(2);
	}

	@Test
	void closedDayHasNoSlots() {
		OperatingHours closed = new OperatingHours(DayOfWeek.SATURDAY, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT, true);
		assertThat(slots(closed, 60, List.of(rule(null, "00:00", "23:59", 1000, 0)))).isEmpty();
		assertThat(slots(null, 60, List.of(rule(null, "00:00", "23:59", 1000, 0)))).isEmpty();
	}

	@Test
	void higherPriorityRuleWins() {
		List<Slot> slots = slots(hours("16:00", "18:00"), 60,
				List.of(rule(null, "06:00", "22:00", 1200, 0), rule(null, "17:00", "22:00", 1800, 1, PriceLabel.PEAK)));

		assertThat(slots.get(0).price()).isEqualByComparingTo("1200");
		assertThat(slots.get(0).label()).isEqualTo(PriceLabel.STANDARD);
		assertThat(slots.get(1).price()).isEqualByComparingTo("1800");
		assertThat(slots.get(1).label()).isEqualTo(PriceLabel.PEAK);
	}

	@Test
	void daySpecificRuleBeatsEveryDayRuleAtSamePriority() {
		List<Slot> slots = slots(hours("06:00", "07:00"), 60,
				List.of(rule(DayOfWeek.SATURDAY, "06:00", "22:00", 1500, 0), rule(null, "06:00", "22:00", 1200, 0)));
		assertThat(slots.getFirst().price()).isEqualByComparingTo("1500");

		List<Slot> otherDay = slots(hours("06:00", "07:00"), 60,
				List.of(rule(DayOfWeek.MONDAY, "06:00", "22:00", 1500, 0), rule(null, "06:00", "22:00", 1200, 0)));
		assertThat(otherDay.getFirst().price()).isEqualByComparingTo("1200");
	}

	@Test
	void ruleOnlyAppliesWithinItsValidDates() {
		PriceRule holiday = new PriceRule(UUID.randomUUID(), PriceLabel.HOLIDAY, null, LocalTime.of(6, 0),
				LocalTime.of(22, 0), new BigDecimal("2000"), 5, DATE, DATE);
		PriceRule standard = rule(null, "06:00", "22:00", 1200, 0);

		assertThat(SlotCalculator.priceFor(DATE, LocalTime.of(10, 0), List.of(holiday, standard)))
			.hasValueSatisfying(rule -> assertThat(rule.label()).isEqualTo(PriceLabel.HOLIDAY));
		assertThat(SlotCalculator.priceFor(DATE.plusDays(1), LocalTime.of(10, 0), List.of(holiday, standard)))
			.hasValueSatisfying(rule -> assertThat(rule.label()).isEqualTo(PriceLabel.STANDARD));
	}

	@Test
	void slotsWithoutAPriceAreNotOffered() {
		List<Slot> slots = slots(hours("06:00", "10:00"), 60, List.of(rule(null, "08:00", "10:00", 1000, 0)));

		assertThat(slots).extracting(slot -> slot.startsAt().toLocalTime())
			.containsExactly(LocalTime.of(8, 0), LocalTime.of(9, 0));
	}

	@Test
	void slotStatusReflectsBookingsBlocksAndTime() {
		TimeRange booking = range("07:30", "08:30");
		TimeRange block = range("10:00", "11:00");
		Instant now = at("09:00").toInstant();

		List<Slot> slots = SlotCalculator.slots(DATE, KATHMANDU, hours("06:00", "12:00"), 60,
				List.of(rule(null, "00:00", "23:59", 1000, 0)), List.of(booking, range("11:00", "12:00")),
				List.of(block, range("11:00", "12:00")), now);

		assertThat(slots).extracting(Slot::status)
			.containsExactly(SlotStatus.PAST, SlotStatus.BOOKED, SlotStatus.BOOKED, SlotStatus.PAST,
					SlotStatus.BLOCKED, SlotStatus.BLOCKED);
	}

	@Test
	void backToBackBookingDoesNotTouchNeighbouringSlot() {
		List<Slot> slots = SlotCalculator.slots(DATE, KATHMANDU, hours("06:00", "08:00"), 60,
				List.of(rule(null, "00:00", "23:59", 1000, 0)), List.of(range("06:00", "07:00")), List.of(), LONG_AGO);

		assertThat(slots).extracting(Slot::status).containsExactly(SlotStatus.BOOKED, SlotStatus.AVAILABLE);
	}

	private static List<Slot> slots(OperatingHours hours, int minutes, List<PriceRule> rules) {
		return SlotCalculator.slots(DATE, KATHMANDU, hours, minutes, rules, List.of(), List.of(), LONG_AGO);
	}

	private static OperatingHours hours(String open, String close) {
		return new OperatingHours(DATE.getDayOfWeek(), LocalTime.parse(open), LocalTime.parse(close), false);
	}

	private static PriceRule rule(DayOfWeek day, String start, String end, int price, int priority) {
		return rule(day, start, end, price, priority, PriceLabel.STANDARD);
	}

	private static PriceRule rule(DayOfWeek day, String start, String end, int price, int priority, PriceLabel label) {
		return new PriceRule(UUID.randomUUID(), label, day, LocalTime.parse(start), LocalTime.parse(end),
				new BigDecimal(price), priority, null, null);
	}

	private static TimeRange range(String start, String end) {
		return new TimeRange(at(start).toInstant(), at(end).toInstant());
	}

	private static ZonedDateTime at(String time) {
		return ZonedDateTime.of(DATE, LocalTime.parse(time), KATHMANDU);
	}

}
