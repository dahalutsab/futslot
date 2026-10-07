package com.futslot.site;

import java.util.List;

/**
 * Everything a site template shows, already worded and formatted, so templates only lay it out. Lists are empty
 * (never null) when there's nothing to show; optional single values are null. {@code accentText} is the accent
 * adjusted, if needed, to be readable as text on the template's background.
 */
public record SitePage(String template, String accent, String accentInk, String accentText, Seo seo, Venue venue,
		Contact contact, List<Photo> photos, Pricing pricing, Slots slots, List<HoursRow> hours, List<String> amenities, Rating rating,
		List<Review> reviews, List<Plan> plans, List<Tournament> tournaments, int year) {

	/** {@code canonical} is the site's own address; {@code noindex} keeps previews out of search results. */
	public record Seo(String title, String description, String canonical, String image, String jsonLd, boolean noindex) {
	}

	/** {@code openNow}: "Open now, until 10 PM", "Opens at 6 AM" or "Closed today". */
	public record Venue(String name, String headline, String about, String place, String address, String cover,
			int courtCount, String formats, String todayHours, String openNow, boolean open) {
	}

	/** {@code whatsapp} opens a chat with the venue; {@code tel} calls it. Null when the venue has no phone. */
	public record Contact(String phone, String tel, String whatsapp, String mapEmbed, String directions) {
	}

	public record Photo(String url, String caption) {
	}

	public record Pricing(String from, List<Court> courts) {
	}

	public record Court(String name, String details, List<Price> prices) {
	}

	public record Price(String label, String when, String amount) {
	}

	/**
	 * Free slots for one day. {@code days} are the next few days to switch between; each slot's {@code book}
	 * link starts a booking. {@code online}: slots link to booking on the site (otherwise to a WhatsApp message, in
	 * previews); {@code mine} links to the signed-in player's bookings, or is null.
	 */
	public record Slots(String dayLabel, List<Day> days, List<CourtSlots> courts, List<BoardRow> board, int free,
			boolean online, String mine) {
	}

	/** One free slot on the day's combined board, earliest first across all courts. */
	public record BoardRow(String time, String court, String price, String book) {
	}

	public record Day(String label, String date, String href, boolean selected) {
	}

	public record CourtSlots(String name, List<Slot> slots) {
	}

	public record Slot(String time, String price, String book) {
	}

	public record HoursRow(String day, String hours, boolean today) {
	}

	public record Rating(String average, int count, int rounded) {
	}

	public record Review(String name, int rating, String comment, String played, String reply) {
	}

	public record Plan(String name, String summary, String price, String description) {
	}

	public record Tournament(String name, String dates, String entry, String spots, String format) {
	}

}
