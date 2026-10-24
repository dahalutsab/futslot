package com.futslot.site;

import java.util.List;

/**
 * What FutSlot's own public pages show (the front page, a city's or an area's futsal venues), already worded in
 * the page's language. {@code base} is what links to other public pages start with ("" in English, "/ne" in
 * Nepali); links into the player app (/explore, /login...) don't take it, the app keeps its own language.
 * {@code switchHref} is this page in the other language. {@code cheapest} is the lowest game price among the page's
 * venues ("Rs 1,200"), or null. Lists are empty, never null.
 */
public record DirectoryPage(String lang, String base, String switchHref, SitePage.Seo seo, String breadcrumbsJsonLd,
		List<Crumb> crumbs, String heading, String intro, String cheapest, List<Place> places, List<Card> venues,
		List<Place> cities, String owners, String year) {

	/** One step of "FutSlot › Kathmandu › New Baneshwor"; the last one, the page itself, has no link. */
	public record Crumb(String name, String href) {
	}

	/** A city or area with venues: "Kathmandu", its page, and "3 venues". */
	public record Place(String name, String href, String count) {
	}

	/**
	 * A venue in a list. {@code from} is "From Rs 1,200" or null; {@code rating} is "4.5 (12)" or null;
	 * {@code cover} is the cover photo's address or null.
	 */
	public record Card(String name, String href, String place, String from, String courts, String rating, String cover,
			String alt) {
	}

}
