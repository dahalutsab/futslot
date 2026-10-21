package com.futslot.common;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/** Hands out {@link Wording} in English or Nepali. */
@Component
public class Words {

	private final MessageSource messages;

	Words(MessageSource messages) {
		this.messages = messages;
	}

	public Wording in(Locale locale) {
		return new Wording(this.messages, Languages.isNepali(locale) ? Languages.NEPALI : Languages.ENGLISH);
	}

	/** For a user's preferredLanguage ("en" or "ne"). */
	public Wording in(String language) {
		return in(Languages.of(language));
	}

	/**
	 * For whoever made the request being handled: the language its page chose, otherwise its Accept-Language (see
	 * the LocaleResolver in LocaleConfig); English outside a request.
	 */
	public Wording forRequest() {
		return in(LocaleContextHolder.getLocale());
	}

}
