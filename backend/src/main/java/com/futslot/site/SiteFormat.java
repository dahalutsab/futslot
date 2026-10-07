package com.futslot.site;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;

/** How venue sites write money, times and days: the way people in Nepal say them. */
final class SiteFormat {

	private SiteFormat() {
	}

	/** "Rs 1,200", "Rs 1,20,000" (lakh grouping); paisa only when there are any. */
	static String rupees(BigDecimal amount) {
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
		return "Rs " + (value.signum() < 0 ? "-" : "") + grouped + fraction;
	}

	/** "6 AM", "6:30 PM", "12 AM" for midnight. */
	static String clock(LocalTime time) {
		int hour = time.getHour() % 12 == 0 ? 12 : time.getHour() % 12;
		String suffix = time.getHour() < 12 ? "AM" : "PM";
		return time.getMinute() == 0 ? hour + " " + suffix : "%d:%02d %s".formatted(hour, time.getMinute(), suffix);
	}

	/** "6 AM – 10 PM"; an end at the last minute of the day reads as midnight. */
	static String span(LocalTime from, LocalTime to) {
		LocalTime end = (to.getHour() == 23 && to.getMinute() == 59) ? LocalTime.MIDNIGHT : to;
		return clock(from) + " – " + clock(end);
	}

	/** "Thu 9 Oct". */
	static String day(LocalDate date) {
		return date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + date.getDayOfMonth() + " "
				+ date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
	}

	/** "Sunday". */
	static String weekday(DayOfWeek day) {
		return day.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
	}

	/** "Fridays" for a single day, "Every day" for none. */
	static String days(DayOfWeek day) {
		return (day == null) ? "Every day" : weekday(day) + "s";
	}

	/** Black or white, whichever has more contrast on {@code hex} (they're equal at luminance 0.179). */
	static String inkOn(String hex) {
		int r = Integer.parseInt(hex.substring(1, 3), 16);
		int g = Integer.parseInt(hex.substring(3, 5), 16);
		int b = Integer.parseInt(hex.substring(5, 7), 16);
		double luminance = (0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b));
		return luminance > 0.179 ? "#0b0b0b" : "#ffffff";
	}

	/**
	 * {@code hex} if text in it is readable (4.5:1) on {@code background}, otherwise the nearest version mixed
	 * towards white (on dark backgrounds) or black (on light ones) that is.
	 */
	static String readableOn(String hex, String background) {
		double backgroundLuminance = luminance(background);
		int toward = backgroundLuminance < 0.5 ? 255 : 0;
		int r = Integer.parseInt(hex.substring(1, 3), 16);
		int g = Integer.parseInt(hex.substring(3, 5), 16);
		int b = Integer.parseInt(hex.substring(5, 7), 16);
		for (int step = 0; step <= 20; step++) {
			double mix = step / 20.0;
			String candidate = "#%02x%02x%02x".formatted(Math.round(r + (toward - r) * mix), Math.round(g + (toward - g) * mix),
					Math.round(b + (toward - b) * mix));
			if (contrast(luminance(candidate), backgroundLuminance) >= 4.5) {
				return candidate;
			}
		}
		return toward == 255 ? "#ffffff" : "#000000";
	}

	private static double contrast(double a, double b) {
		return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
	}

	private static double luminance(String hex) {
		return 0.2126 * channel(Integer.parseInt(hex.substring(1, 3), 16)) + 0.7152 * channel(Integer.parseInt(hex.substring(3, 5), 16))
				+ 0.0722 * channel(Integer.parseInt(hex.substring(5, 7), 16));
	}

	private static double channel(int value) {
		double c = value / 255.0;
		return (c <= 0.03928) ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
	}

}
