package com.futslot.site;

import java.util.List;

/**
 * A booking page on a venue's website. {@code step} says what the page shows: "phone" and "code" to sign in,
 * "confirm" to book, "gone" when the slot was taken meanwhile, "booking" for a booking already made, "list" for
 * the player's bookings at the venue and "deposit" for topping up their FutSlot booking deposit.
 * {@code base} is what the site's links start with ("" on the venue's own address).
 */
public record BookPage(Site site, String step, Slot slot, String phone, boolean needsName, String error, String notice,
		boolean signedIn, Booking booking, List<Booking> bookings, Deposit deposit) {

	BookPage(Site site, String step, Slot slot, String phone, boolean needsName, String error, String notice,
			boolean signedIn, Booking booking, List<Booking> bookings) {
		this(site, step, slot, phone, needsName, error, notice, signedIn, booking, bookings, null);
	}

	public record Site(String name, String template, String fonts, String accent, String accentInk, String accentText,
			String base) {
	}

	/**
	 * {@code advance} is null when nothing is paid up front. {@code strikeAdvance}: the advance is higher because of
	 * the player's recent late cancellations and no-shows. {@code depositRequired}: the venue needs a full FutSlot
	 * booking deposit; {@code depositToAdd} is what the signed-in player must add first, or null.
	 */
	public record Slot(String courtId, String start, String court, String day, String time, String price, String advance,
			String holdMinutes, boolean strikeAdvance, boolean depositRequired, String depositToAdd) {
	}

	/**
	 * The player's FutSlot booking deposit. {@code toAdd} is null when it's full; {@code checking}: a top-up was
	 * sent and FutSlot hasn't checked it yet. {@code court} and {@code start} lead back to the slot they were
	 * booking, if any.
	 */
	public record Deposit(String required, String balance, String toAdd, String toAddAmount, boolean checking,
			String instructions, List<Method> methods, String court, String start) {
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
