package com.futslot.slot;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.futslot.venue.OperatingHours;

/**
 * Turns a court's opening hours, slot length and pricing into the bookable slots of one operating day.
 *
 * <p>An operating day starts at its opening time; if closing is at or before opening the day runs past
 * midnight, and those late slots still belong to (and are priced as) the day they started from. Slots
 * are aligned to the opening time; a trailing remainder shorter than one slot is not offered. A slot
 * with no matching price rule is not offered either.
 */
public final class SlotCalculator {

	/** Highest priority first, then day-specific over every-day, then the most recently created (UUIDv7 order). */
	private static final Comparator<PriceRule> PRECEDENCE = Comparator.comparingInt(PriceRule::priority)
		.thenComparing(rule -> rule.dayOfWeek() != null)
		.thenComparing(PriceRule::id)
		.reversed();

	private SlotCalculator() {
	}

	public static List<Slot> slots(LocalDate operatingDate, ZoneId zone, OperatingHours hours, int slotMinutes,
			List<PriceRule> rules, List<TimeRange> booked, List<TimeRange> blocked, Instant now) {
		if (hours == null || hours.isClosed()) {
			return List.of();
		}
		ZonedDateTime open = ZonedDateTime.of(operatingDate, hours.openTime(), zone);
		LocalDate closeDate = hours.closeTime().isAfter(hours.openTime()) ? operatingDate : operatingDate.plusDays(1);
		ZonedDateTime close = ZonedDateTime.of(closeDate, hours.closeTime(), zone);

		List<Slot> slots = new ArrayList<>();
		for (ZonedDateTime start = open; !start.plusMinutes(slotMinutes).isAfter(close); start = start.plusMinutes(slotMinutes)) {
			ZonedDateTime end = start.plusMinutes(slotMinutes);
			Optional<PriceRule> rule = priceFor(operatingDate, start.toLocalTime(), rules);
			if (rule.isEmpty()) {
				continue;
			}
			TimeRange range = new TimeRange(start.toInstant(), end.toInstant());
			slots.add(new Slot(start.toOffsetDateTime(), end.toOffsetDateTime(), rule.get().price(), rule.get().label(),
					status(range, booked, blocked, now)));
		}
		return slots;
	}

	static Optional<PriceRule> priceFor(LocalDate operatingDate, LocalTime slotStart, List<PriceRule> rules) {
		return rules.stream().filter(rule -> rule.appliesTo(operatingDate, slotStart)).min(PRECEDENCE);
	}

	private static SlotStatus status(TimeRange slot, List<TimeRange> booked, List<TimeRange> blocked, Instant now) {
		if (blocked.stream().anyMatch(slot::overlaps)) {
			return SlotStatus.BLOCKED;
		}
		if (booked.stream().anyMatch(slot::overlaps)) {
			return SlotStatus.BOOKED;
		}
		if (!slot.start().isAfter(now)) {
			return SlotStatus.PAST;
		}
		return SlotStatus.AVAILABLE;
	}

}
