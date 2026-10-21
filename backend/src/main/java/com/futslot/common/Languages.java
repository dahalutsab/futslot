package com.futslot.common;

import java.util.Locale;

/** The languages FutSlot speaks to players in: English, and Nepali. */
public final class Languages {

	public static final Locale ENGLISH = Locale.ENGLISH;

	public static final Locale NEPALI = Locale.of("ne");

	/**
	 * Request attribute holding the {@link Locale} a page has chosen for itself (a venue site's Nepali address, say),
	 * which wins over the browser's Accept-Language.
	 */
	public static final String CHOSEN = Languages.class.getName() + ".CHOSEN";

	private Languages() {
	}

	/** Nepali for "ne", English for anything else. */
	public static Locale of(String code) {
		return "ne".equals(code) ? NEPALI : ENGLISH;
	}

	public static boolean isNepali(Locale locale) {
		return NEPALI.getLanguage().equals(locale.getLanguage());
	}

}
