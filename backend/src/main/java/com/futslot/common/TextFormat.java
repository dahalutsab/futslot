package com.futslot.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;

/**
 * How FutSlot writes money, times and days to players: the way people in Nepal say them, in English or in Nepali
 * (Devanagari digits, the part of the day before the hour, Bikram Sambat dates). Words other than those live in the
 * message bundles (see {@link Wording}).
 */
public final class TextFormat {

	public static final TextFormat ENGLISH = new TextFormat(false);

	public static final TextFormat NEPALI = new TextFormat(true);

	private final boolean nepali;

	private TextFormat(boolean nepali) {
		this.nepali = nepali;
	}

	public static TextFormat of(Locale locale) {
		return Languages.isNepali(locale) ? NEPALI : ENGLISH;
	}

	/** "42", or "४२" in Nepali. */
	public String number(Object value) {
		return this.nepali ? Nepali.digits(String.valueOf(value)) : String.valueOf(value);
	}

	/** "Rs 1,200", "Rs 1,20,000" (lakh grouping), "रु १,२००" in Nepali; paisa only when there are any. */
	public String rupees(BigDecimal amount) {
		BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
		String plain = value.abs().toPlainString();
		String whole = plain.contains(".") ? plain.substring(0, plain.indexOf('.')) : plain;
		String fraction = plain.contains(".") ? plain.substring(plain.indexOf('.')) : "";
		StringBuilder grouped = new StringBuilder();
		int length = whole.length();
		for (int i = 0; i < length; i++) {
			int fromEnd = length - i;
			grouped.append(whole.charAt(i));
			if (fromEnd > 1 && (fromEnd == 4 || (fromEnd > 4 && (fromEnd - 4) % 2 == 0))) {
				grouped.append(',');
			}
		}
		return (this.nepali ? "रु " : "Rs ") + number((value.signum() < 0 ? "-" : "") + grouped + fraction);
	}

	/** "18:00", "06:30", "00:00" for midnight, the 24-hour clock; "बिहान ६ बजे", "बेलुका ६:३० बजे" in Nepali, as the player app says them. */
	public String clock(LocalTime time) {
		int hour = time.getHour() % 12 == 0 ? 12 : time.getHour() % 12;
		String text = time.getMinute() == 0 ? String.valueOf(hour) : "%d:%02d".formatted(hour, time.getMinute());
		if (this.nepali) {
			int h = time.getHour();
			String part = (h >= 4 && h < 12) ? "बिहान" : (h >= 12 && h < 16) ? "दिउँसो" : (h >= 16 && h < 20) ? "बेलुका" : "राति";
			return part + " " + Nepali.digits(text) + " बजे";
		}
		return "%02d:%02d".formatted(time.getHour(), time.getMinute());
	}

	/** "06:00 – 22:00"; an end at the last minute of the day reads as midnight. */
	public String span(LocalTime from, LocalTime to) {
		LocalTime end = (to.getHour() == 23 && to.getMinute() == 59) ? LocalTime.MIDNIGHT : to;
		return clock(from) + " – " + clock(end);
	}

	/** "Thu 9 Oct"; "बिहि, २३ आश्विन" in Nepali. */
	public String day(LocalDate date) {
		Optional<Nepali.BsDate> bs = this.nepali ? Nepali.bs(date) : Optional.empty();
		if (bs.isPresent()) {
			return Nepali.weekdayShort(date.getDayOfWeek()) + ", " + Nepali.digits(bs.get().day()) + " " + bs.get().monthName();
		}
		return number(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + date.getDayOfMonth() + " "
				+ date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
	}

	/** "9 Oct", without the weekday; "२३ आश्विन" in Nepali. */
	public String dayMonth(LocalDate date) {
		Optional<Nepali.BsDate> bs = this.nepali ? Nepali.bs(date) : Optional.empty();
		if (bs.isPresent()) {
			return Nepali.digits(bs.get().day()) + " " + bs.get().monthName();
		}
		return number(date.getDayOfMonth() + " " + date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
	}

	/** "Thu 9", without the month, for a row of days to choose from; "बिहि २३" in Nepali. */
	public String dayOfWeek(LocalDate date) {
		Optional<Nepali.BsDate> bs = this.nepali ? Nepali.bs(date) : Optional.empty();
		if (bs.isPresent()) {
			return Nepali.weekdayShort(date.getDayOfWeek()) + " " + Nepali.digits(bs.get().day());
		}
		return number(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + date.getDayOfMonth());
	}

	/** "Sunday"; "आइतबार" in Nepali. */
	public String weekday(DayOfWeek day) {
		return this.nepali ? Nepali.weekday(day) : day.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
	}

	/** "Saturday 25 October 2026", for a dateline; "शनिबार, ८ कात्तिक २०८३" in Nepali. */
	public String longDate(LocalDate date) {
		Optional<Nepali.BsDate> bs = this.nepali ? Nepali.bs(date) : Optional.empty();
		if (bs.isPresent()) {
			return Nepali.weekday(date.getDayOfWeek()) + ", " + Nepali.digits(bs.get().day()) + " " + bs.get().monthName() + " "
					+ Nepali.digits(bs.get().year());
		}
		return weekday(date.getDayOfWeek()) + " " + date.getDayOfMonth() + " "
				+ date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + date.getYear();
	}

	/** The year {@code date} falls in: Bikram Sambat in Nepali. */
	public String year(LocalDate date) {
		return this.nepali ? Nepali.bs(date).map(bs -> Nepali.digits(bs.year())).orElse(number(date.getYear()))
				: String.valueOf(date.getYear());
	}

}
