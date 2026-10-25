package com.futslot.site;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * What FutSlot's own public pages show (the front page, a city's or an area's futsal venues), already worded in
 * the page's language. {@code base} is what links to other public pages start with ("" in English, "/ne" in
 * Nepali); links into the player app (/explore, /login...) don't take it, the app keeps its own language.
 * {@code switchHref} is this page in the other language. {@code cheapest} is the lowest game price among the page's
 * venues ("Rs 1,200"), or null. The front page also has a {@code dateline} ("Saturday 25 October 2026", "Kathmandu
 * Valley", "14 venues · 23 courts", "Games from Rs 1,000") and {@code freeNext}, the soonest free hour at each venue,
 * soonest first, headed {@code freeNextTitle} ("Free next", or "Free tomorrow" once today's are gone). Lists are
 * empty, never null.
 */
public record DirectoryPage(String lang, String base, String switchHref, SitePage.Seo seo, String breadcrumbsJsonLd,
		List<Crumb> crumbs, String heading, String intro, String cheapest, List<Place> places, List<Card> venues,
		List<Place> cities, String owners, String year, List<String> dateline, String freeNextTitle, List<Next> freeNext) {

	/** One step of "FutSlot › Kathmandu › New Baneshwor"; the last one, the page itself, has no link. */
	public record Crumb(String name, String href) {
	}

	/** A city or area with venues: "Kathmandu", its page, "3 venues" and the bare "3". */
	public record Place(String name, String href, String count, String number) {
	}

	/**
	 * A venue in a list. {@code area} is its area (or city when it has none); {@code headline} its website's
	 * headline or null. {@code from} is "From Rs 1,200" or null; {@code rating} is "4.5 (12)" or null;
	 * {@code cover} is the cover photo's address or null. {@code freeWhen} is "Free today" (or tomorrow, once today's
	 * games are over) with the next free hours, which book straight away; null when none are free.
	 */
	public record Card(String name, String href, String place, String area, String headline, String from, String courts,
			String rating, String cover, String alt, String freeWhen, List<Hour> free) {
	}

	/** A free hour: "19:00", where booking it starts, its price ("Rs 1,800"), the court free then and when it starts. */
	public record Hour(String time, String book, String price, String court, OffsetDateTime startsAt) {
	}

	/** A row of the front page's "Free next" index: "7 PM", the venue, its area, the hour's price and booking it. */
	public record Next(String time, String venue, String area, String price, String book) {
	}

}
