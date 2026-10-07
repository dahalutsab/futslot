package com.futslot.notification;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/** Wording of user-facing notifications, kept in one place for review and later translation. */
final class Messages {

	private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.ENGLISH);

	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

	private Messages() {
	}

	static String when(OffsetDateTime time) {
		return WHEN.format(time);
	}

	static String time(OffsetDateTime time) {
		return TIME.format(time);
	}

	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);

	static String day(LocalDate date) {
		return DAY.format(date);
	}

	static String date(LocalDate date) {
		return DATE.format(date);
	}

	/** "Fridays 19:00-20:00". */
	static String weekly(DayOfWeek day, LocalTime start, LocalTime end) {
		String name = day.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
		return name + "s " + start + "-" + end;
	}

	/** The start of a player-written text, cut at a word boundary. */
	static String excerpt(String text) {
		String flat = text.strip().replaceAll("\\s+", " ");
		if (flat.length() <= 80) {
			return flat;
		}
		int cut = flat.lastIndexOf(' ', 80);
		return flat.substring(0, (cut > 40) ? cut : 80) + "...";
	}

	static String rupees(BigDecimal amount) {
		return "Rs " + amount.stripTrailingZeros().toPlainString();
	}

}
