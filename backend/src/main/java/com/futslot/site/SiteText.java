package com.futslot.site;

import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;

import com.futslot.common.Languages;

/** A site page's wording in one language: messages from i18n/sites*.properties, and how it writes numbers and days. */
final class SiteText {

	private final MessageSource messages;

	private final Locale locale;

	private final SiteFormat format;

	SiteText(MessageSource messages, Locale locale) {
		this.messages = messages;
		this.locale = locale;
		this.format = SiteFormat.of(locale);
	}

	/** The message for {@code key}, with {@code values} (already formatted) in place of {0}, {1}... */
	String get(String key, Object... values) {
		return this.messages.getMessage(key, values, this.locale);
	}

	/** The message for {@code key}, or {@code fallback} if there's none (an amenity a venue named itself, say). */
	String orElse(String key, String fallback) {
		return this.messages.getMessage(key, null, fallback, this.locale);
	}

	SiteFormat format() {
		return this.format;
	}

	boolean nepali() {
		return Languages.isNepali(this.locale);
	}

	/** "en" or "ne", for the page's lang attribute. */
	String code() {
		return nepali() ? "ne" : "en";
	}

	/** "a", "a and b", "a, b and c". */
	String list(List<String> items) {
		if (items.size() < 2) {
			return items.isEmpty() ? "" : items.getFirst();
		}
		return get("and.two", String.join(", ", items.subList(0, items.size() - 1)), items.getLast());
	}

}
