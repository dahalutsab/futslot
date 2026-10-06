package com.futslot.notification;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
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

	static String rupees(BigDecimal amount) {
		return "Rs " + amount.stripTrailingZeros().toPlainString();
	}

}
