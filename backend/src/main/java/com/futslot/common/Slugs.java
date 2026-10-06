package com.futslot.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

public final class Slugs {

	private Slugs() {
	}

	/** "Dhuku Futsal & Café" -> "dhuku-futsal-cafe"; appends -2, -3... until {@code taken} says no. */
	public static String unique(String text, String fallback, Predicate<String> taken) {
		String base = Normalizer.normalize(text, Normalizer.Form.NFD)
			.replaceAll("\\p{M}", "")
			.toLowerCase(Locale.ROOT)
			.replaceAll("[^a-z0-9]+", "-")
			.replaceAll("(^-|-$)", "");
		if (base.isEmpty()) {
			base = fallback;
		}
		if (base.length() > 60) {
			base = base.substring(0, 60).replaceAll("-$", "");
		}
		String slug = base;
		for (int n = 2; taken.test(slug); n++) {
			slug = base + "-" + n;
		}
		return slug;
	}

}
