package com.futslot.site;

/** Colours for venue sites: text that stays readable on a venue's chosen accent. Words and numbers are TextFormat's. */
final class SiteFormat {

	private SiteFormat() {
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
