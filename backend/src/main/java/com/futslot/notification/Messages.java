package com.futslot.notification;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

import com.futslot.common.TextFormat;
import com.futslot.common.Wording;

/**
 * How notifications write times, days and money. Those to players take their {@link Wording}: in English as
 * always, in Nepali the way the apps write them (see {@link TextFormat}). Those to venue staff stay in English.
 */
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

	/** "Fri 9 Oct, 18:00"; "शुक्र, २३ आश्विन, बेलुका ६ बजे" in Nepali. */
	static String when(OffsetDateTime time, Wording words) {
		return words.nepali() ? words.format().day(time.toLocalDate()) + ", " + words.format().clock(time.toLocalTime())
				: when(time);
	}

	/** "18:00"; "बेलुका ६ बजे" in Nepali. */
	static String time(OffsetDateTime time, Wording words) {
		return words.nepali() ? words.format().clock(time.toLocalTime()) : time(time);
	}

	/** "9 Oct"; "२३ आश्विन" in Nepali. */
	static String day(LocalDate date, Wording words) {
		return words.nepali() ? words.format().dayMonth(date) : day(date);
	}

	/** "Fri 9 Oct"; "शुक्र, २३ आश्विन" in Nepali. */
	static String date(LocalDate date, Wording words) {
		return words.nepali() ? words.format().day(date) : date(date);
	}

	/** "Fridays 19:00-20:00"; "हरेक शुक्रबार बेलुका ७ बजे – राति ८ बजे" in Nepali. */
	static String weekly(DayOfWeek day, LocalTime start, LocalTime end, Wording words) {
		return words.nepali() ? "हरेक " + words.format().weekday(day) + " " + words.format().span(start, end)
				: weekly(day, start, end);
	}

	/** "Rs 1200"; "रु १,२००" in Nepali. */
	static String rupees(BigDecimal amount, Wording words) {
		return words.nepali() ? words.format().rupees(amount) : rupees(amount);
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
