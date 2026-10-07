package com.futslot.site;

import java.util.List;

/**
 * A booking page on a venue's website. {@code step} says what the page shows: "phone" and "code" to sign in,
 * "confirm" to book, "gone" when the slot was taken meanwhile, "booking" for a booking already made and "list" for
 * the player's bookings at the venue.
 * {@code base} is what the site's links start with ("" on the venue's own address).
 */
public record BookPage(Site site, String step, Slot slot, String phone, boolean needsName, String error, String notice,
		boolean signedIn, Booking booking, List<Booking> bookings) {

	public record Site(String name, String template, String fonts, String accent, String accentInk, String accentText,
			String base) {
	}

	/** {@code advance} is null when nothing is paid up front. */
	public record Slot(String courtId, String start, String court, String day, String time, String price, String advance,
			String holdMinutes) {
	}

	/**
	 * {@code payNow}: the advance still to pay before the hold runs out at {@code holdUntil}, or null when
	 * nothing is due now. {@code checking}: a payment has been sent and the venue is checking it.
	 */
	public record Booking(String id, String reference, String status, String tone, String court, String day, String time,
			String total, String paid, String payNow, String payNowAmount, String holdUntil, boolean checking,
			String instructions, List<Method> methods) {
	}

	public record Method(String value, String label) {
	}

}
