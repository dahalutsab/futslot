package com.futslot.common;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;

/**
 * What FutSlot says to someone, in their language: messages from the i18n bundles, and how numbers and days are
 * written (see {@link TextFormat}).
 */
public final class Wording {

	private final MessageSource messages;

	private final Locale locale;

	private final TextFormat format;

	Wording(MessageSource messages, Locale locale) {
		this.messages = messages;
		this.locale = locale;
		this.format = TextFormat.of(locale);
	}

	/** The message for {@code key}, with {@code values} (already formatted) in place of {0}, {1}... */
	public String get(String key, Object... values) {
		return this.messages.getMessage(key, values, this.locale);
	}

	/** The message for {@code key}, or {@code fallback} if there's none (an amenity a venue named itself, say). */
	public String orElse(String key, String fallback, Object... values) {
		return this.messages.getMessage(key, values, fallback, this.locale);
	}

	public TextFormat format() {
		return this.format;
	}

	public Locale locale() {
		return this.locale;
	}

	public boolean nepali() {
		return Languages.isNepali(this.locale);
	}

	/** "en" or "ne", as stored in a user's preferredLanguage and written in a page's lang attribute. */
	public String code() {
		return nepali() ? "ne" : "en";
	}

	/**
	 * An error's message in this language: its own wording if there is one, otherwise the English message. Its
	 * values are written as this language writes them: amounts (BigDecimal) in rupees, days as dates, other numbers
	 * in this language's digits.
	 */
	public String error(ApiException ex) {
		if (!nepali()) {
			return ex.getMessage();
		}
		Object[] values = new Object[ex.getValues().length];
		for (int i = 0; i < values.length; i++) {
			Object value = ex.getValues()[i];
			values[i] = switch (value) {
				case BigDecimal amount -> this.format.rupees(amount);
				case Number number -> this.format.number(number);
				case LocalDate day -> this.format.day(day);
				case null -> "";
				default -> value.toString();
			};
		}
		// A null default rather than the English message: Spring would run a default through MessageFormat too
		String worded = this.messages.getMessage(ex.getKey(), values, null, this.locale);
		return (worded != null) ? worded : ex.getMessage();
	}

	/** "a", "a and b", "a, b and c". */
	public String list(List<String> items) {
		if (items.size() < 2) {
			return items.isEmpty() ? "" : items.getFirst();
		}
		return get("and.two", String.join(", ", items.subList(0, items.size() - 1)), items.getLast());
	}

}
