package com.futslot.site;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.futslot.common.TextFormat;
import com.futslot.common.Wording;
import com.futslot.slot.AvailabilityResponse;
import com.futslot.slot.AvailabilityService;
import com.futslot.slot.SlotStatus;
import com.futslot.venue.VenueService;
import com.futslot.venue.VenueSummaryResponse;

import tools.jackson.databind.json.JsonMapper;

/**
 * FutSlot's own public pages: the front page, a page for each city and area with venues, and each venue's page,
 * in English and beneath /ne in Nepali. They're what search engines find for "futsal in Kathmandu", so they're
 * whole pages from the server; booking and accounts are in the player app they link to.
 */
@Service
class DirectoryService {

	/** Every live venue is read for each page; well past what one country's futsal venues number. */
	private static final int MAX_VENUES = 2000;

	/** Venues in a list whose next free hours are looked up, and how many hours each shows. */
	private static final int FREE_FOR = 24;

	private static final int FREE_HOURS = 4;

	private final VenueService venues;

	private final AvailabilityService availability;

	private final SitePageService pages;

	private final SiteProperties properties;

	private final JsonMapper json;

	private final Clock clock;

	DirectoryService(VenueService venues, AvailabilityService availability, SitePageService pages, SiteProperties properties,
			JsonMapper json, Clock clock) {
		this.venues = venues;
		this.availability = availability;
		this.pages = pages;
		this.properties = properties;
		this.json = json;
		this.clock = clock;
	}

	DirectoryPage home(Wording text) {
		Directory all = directory();
		TextFormat format = text.format();
		String home = this.properties.home();
		String base = base(text);
		List<String> topCities = all.cities().values().stream().limit(3).map(City::name).toList();
		String where = topCities.isEmpty() ? text.get("dir.nepal") : text.list(topCities);
		String count = count(text, "dir.venues", all.venues().size());
		String title = text.get("dir.home.title", where);
		String description = text.get("dir.home.description", count, where);
		Map<String, Object> site = new LinkedHashMap<>();
		site.put("@context", "https://schema.org");
		site.put("@type", "WebSite");
		site.put("name", "FutSlot");
		site.put("url", home + "/");
		site.put("inLanguage", text.code());
		SitePage.Seo seo = new SitePage.Seo(title, description, home + base + "/", home + "/", home + "/ne/", shareImage(),
				write(site), false);
		return new DirectoryPage(text.code(), base, other(text, "/"), seo, null, List.of(), text.get("dir.home.heading"),
				text.get("dir.home.intro", count, where), lowest(all.venues()).map(format::rupees).orElse(null), cityPlaces(all, base, text), cards(all.venues(), base, text),
				cityPlaces(all, base, text), owners(), format.year(today()));
	}

	Optional<DirectoryPage> city(String citySlug, Wording text) {
		Directory all = directory();
		City city = all.cities().get(citySlug);
		if (city == null) {
			return Optional.empty();
		}
		String base = base(text);
		String path = "/futsal/" + city.slug();
		List<DirectoryPage.Place> areas = city.areas()
			.values()
			.stream()
			.map(area -> new DirectoryPage.Place(area.name(), base + path + "/" + area.slug(), count(text, "dir.count", area.venues().size())))
			.toList();
		List<DirectoryPage.Crumb> crumbs = List.of(root(text), new DirectoryPage.Crumb(city.name(), null));
		return Optional.of(place(all, city.name(), city.venues(), areas, path, crumbs, text));
	}

	Optional<DirectoryPage> area(String citySlug, String areaSlug, Wording text) {
		Directory all = directory();
		City city = all.cities().get(citySlug);
		Area area = (city != null) ? city.areas().get(areaSlug) : null;
		if (area == null) {
			return Optional.empty();
		}
		String base = base(text);
		String path = "/futsal/" + city.slug() + "/" + area.slug();
		List<DirectoryPage.Crumb> crumbs = List.of(root(text), new DirectoryPage.Crumb(city.name(), base + "/futsal/" + city.slug()),
				new DirectoryPage.Crumb(area.name(), null));
		return Optional.of(place(all, area.name() + ", " + city.name(), area.venues(), List.of(), path, crumbs, text));
	}

	/**
	 * A venue's page: the same details as its website ({@link SitePageService}), with FutSlot's header around them.
	 * Its free slots link to booking in the player app.
	 */
	Optional<VenuePage> venue(String slug, String date, Wording text) {
		Directory all = directory();
		VenueSummaryResponse venue = all.venues().stream().filter(v -> v.slug().equals(slug)).findFirst().orElse(null);
		if (venue == null) {
			return Optional.empty();
		}
		String home = this.properties.home();
		String base = base(text);
		String path = "/venues/" + slug;
		boolean noindex = !this.properties.searchIndexing();
		String dated = (date != null && date.matches("\\d{4}-\\d{2}-\\d{2}")) ? "?date=" + date : "";
		SitePage page = this.pages.build(slug, SiteTemplate.TURF, null, null, date,
				new SitePageService.Addresses(home, home + path, home + "/ne" + path), "?", noindex, text, other(text, path) + dated, path,
				null);
		String place = StringUtils.hasText(venue.area()) ? venue.area() : venue.city();
		SitePage.Seo seo = page.seo();
		page = page.withSeo(new SitePage.Seo(text.get("dir.venue.title", venue.name(), place), seo.description(), seo.canonical(),
				seo.english(), seo.nepali(), seo.image(), seo.jsonLd(), noindex));
		List<DirectoryPage.Crumb> crumbs = new ArrayList<>();
		crumbs.add(root(text));
		String citySlug = slug(venue.city());
		if (all.cities().containsKey(citySlug)) {
			crumbs.add(new DirectoryPage.Crumb(all.cities().get(citySlug).name(), base + "/futsal/" + citySlug));
			Area area = all.cities().get(citySlug).areas().get(slug(Objects.toString(venue.area(), "")));
			if (area != null) {
				crumbs.add(new DirectoryPage.Crumb(area.name(), base + "/futsal/" + citySlug + "/" + area.slug()));
			}
		}
		crumbs.add(new DirectoryPage.Crumb(venue.name(), null));
		DirectoryPage chrome = new DirectoryPage(text.code(), base, page.switchHref(), page.seo(), breadcrumbs(crumbs),
				List.copyOf(crumbs), venue.name(), null, null, List.of(), List.of(), cityPlaces(all, base, text), owners(),
				text.format().year(today()));
		return Optional.of(new VenuePage(page, chrome, path + "/slots"));
	}

	/**
	 * Every public page, in both languages. With search indexing off (a demo's made-up venues) only the front page
	 * is listed.
	 */
	String sitemap() {
		String home = this.properties.home();
		List<String> paths = new ArrayList<>();
		paths.add("/");
		if (this.properties.searchIndexing()) {
			Directory all = directory();
			for (City city : all.cities().values()) {
				paths.add("/futsal/" + city.slug());
				city.areas().values().forEach(area -> paths.add("/futsal/" + city.slug() + "/" + area.slug()));
			}
			all.venues().forEach(venue -> paths.add("/venues/" + venue.slug()));
		}
		StringBuilder urls = new StringBuilder();
		for (String path : paths) {
			String english = home + path;
			String nepali = home + "/ne" + path;
			String alternates = """
					    <xhtml:link rel="alternate" hreflang="en" href="%s"/>
					    <xhtml:link rel="alternate" hreflang="ne" href="%s"/>
					""".formatted(english, nepali);
			for (String loc : List.of(english, nepali)) {
				urls.append("  <url>\n    <loc>").append(loc).append("</loc>\n").append(alternates).append("  </url>\n");
			}
		}
		return """
				<?xml version="1.0" encoding="UTF-8"?>
				<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">
				%s</urlset>
				""".formatted(urls);
	}

	String robots() {
		return "User-agent: *\nAllow: /\nDisallow: /api/\nSitemap: " + this.properties.home() + "/sitemap.xml\n";
	}

	/**
	 * Part of a city or area name for its address: "New Baneshwor" is new-baneshwor. Empty for names with no
	 * Latin letters or digits, which then get no page of their own.
	 */
	static String slug(String name) {
		String plain = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
	}

	/** A city's or an area's page. */
	private DirectoryPage place(Directory all, String name, List<VenueSummaryResponse> venues, List<DirectoryPage.Place> areas,
			String path, List<DirectoryPage.Crumb> crumbs, Wording text) {
		String home = this.properties.home();
		String base = base(text);
		String count = count(text, "dir.venues", venues.size());
		int courtTotal = venues.stream().mapToInt(VenueSummaryResponse::courtCount).sum();
		String courts = count(text, "dir.courts", courtTotal);
		String from = lowest(venues).map(price -> text.get("dir.fromAGame", text.format().rupees(price))).orElse("");
		boolean noindex = !this.properties.searchIndexing();
		SitePage.Seo seo = new SitePage.Seo(text.get("dir.place.title", name), text.get("dir.place.description", name, count, courts, from),
				home + base + path, noindex ? null : home + path, noindex ? null : home + "/ne" + path, shareImage(), breadcrumbs(crumbs),
				noindex);
		return new DirectoryPage(text.code(), base, other(text, path), seo, null, crumbs, text.get("dir.place.heading", name),
				text.get("dir.place.intro", name, count, courts, from), lowest(venues).map(text.format()::rupees).orElse(null), areas, cards(venues, base, text), cityPlaces(all, base, text),
				owners(), text.format().year(today()));
	}

	/** Live venues, best rated first, grouped by city and area; cities with the most venues first. */
	private Directory directory() {
		List<VenueSummaryResponse> list = this.venues
			.searchActive(null, null, PageRequest.of(0, MAX_VENUES, Sort.by(Sort.Order.desc("averageRating"), Sort.Order.asc("name"))))
			.content();
		Map<String, List<VenueSummaryResponse>> byCity = new LinkedHashMap<>();
		for (VenueSummaryResponse venue : list) {
			String citySlug = slug(venue.city());
			if (!citySlug.isEmpty()) {
				byCity.computeIfAbsent(citySlug, key -> new ArrayList<>()).add(venue);
			}
		}
		Map<String, City> cities = new LinkedHashMap<>();
		byCity.entrySet()
			.stream()
			.sorted(Comparator.comparing((Map.Entry<String, List<VenueSummaryResponse>> e) -> -e.getValue().size())
				.thenComparing(Map.Entry::getKey))
			.forEach(e -> cities.put(e.getKey(), new City(e.getValue().getFirst().city().strip(), e.getKey(), e.getValue(), areas(e.getValue()))));
		return new Directory(list, cities);
	}

	private static Map<String, Area> areas(List<VenueSummaryResponse> venues) {
		Map<String, List<VenueSummaryResponse>> byArea = new LinkedHashMap<>();
		for (VenueSummaryResponse venue : venues) {
			String areaSlug = StringUtils.hasText(venue.area()) ? slug(venue.area()) : "";
			if (!areaSlug.isEmpty()) {
				byArea.computeIfAbsent(areaSlug, key -> new ArrayList<>()).add(venue);
			}
		}
		Map<String, Area> areas = new LinkedHashMap<>();
		byArea.entrySet()
			.stream()
			.sorted(Map.Entry.comparingByKey())
			.forEach(e -> areas.put(e.getKey(), new Area(e.getValue().getFirst().area().strip(), e.getKey(), e.getValue())));
		return areas;
	}

	private List<DirectoryPage.Card> cards(List<VenueSummaryResponse> venues, String base, Wording text) {
		TextFormat format = text.format();
		AtomicInteger looked = new AtomicInteger();
		return venues.stream().map(venue -> {
			Free free = (looked.getAndIncrement() < FREE_FOR) ? free(venue.slug(), text) : Free.NONE;
			String place = StringUtils.hasText(venue.area()) ? venue.area() + ", " + venue.city() : venue.city();
			String from = (venue.fromPrice() != null) ? text.get("dir.from", format.rupees(venue.fromPrice())) : null;
			String rating = (venue.reviewCount() > 0 && venue.averageRating() != null)
					? format.number(venue.averageRating().setScale(1, RoundingMode.HALF_UP).toPlainString()) + " ("
							+ format.number(venue.reviewCount()) + ")"
					: null;
			return new DirectoryPage.Card(venue.name(), base + "/venues/" + venue.slug(), place, from,
					count(text, "dir.courts", venue.courtCount()), rating, venue.coverPhotoUrl(),
					text.get("dir.coverAlt", venue.name(), place), free.when(), free.hours());
		}).toList();
	}

	/** The venue's next free hours today, or tomorrow once today's are gone, across its courts. */
	private Free free(String slug, Wording text) {
		Instant now = this.clock.instant();
		for (int day = 0; day < 2; day++) {
			LocalDate date = today().plusDays(day);
			AvailabilityResponse open;
			try {
				open = this.availability.forPlayers(slug, date);
			}
			catch (RuntimeException ex) {
				return Free.NONE;
			}
			List<DirectoryPage.Hour> hours = open.courts()
				.stream()
				.flatMap(court -> court.slots()
					.stream()
					.filter(slot -> slot.status() == SlotStatus.AVAILABLE && slot.startsAt().toInstant().isAfter(now))
					.map(slot -> Map.entry(slot.startsAt(), court.courtId())))
				// One chip an hour: the first court free then
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new))
				.entrySet()
				.stream()
				.limit(FREE_HOURS)
				.map(e -> new DirectoryPage.Hour(text.format().clock(e.getKey().toLocalTime()), "/venues/" + slug + "/book?court="
						+ e.getValue() + "&start=" + URLEncoder.encode(e.getKey().toString(), StandardCharsets.UTF_8)))
				.toList();
			if (!hours.isEmpty()) {
				return new Free(text.get((day == 0) ? "dir.freeToday" : "dir.freeTomorrow"), hours);
			}
		}
		return Free.NONE;
	}

	private record Free(String when, List<DirectoryPage.Hour> hours) {

		static final Free NONE = new Free(null, List.of());

	}

	private static List<DirectoryPage.Place> cityPlaces(Directory all, String base, Wording text) {
		return all.cities()
			.values()
			.stream()
			.map(city -> new DirectoryPage.Place(city.name(), base + "/futsal/" + city.slug(), count(text, "dir.count", city.venues().size())))
			.toList();
	}

	private static Optional<BigDecimal> lowest(List<VenueSummaryResponse> venues) {
		return venues.stream().map(VenueSummaryResponse::fromPrice).filter(Objects::nonNull).min(Comparator.naturalOrder());
	}

	/** "3 futsal venues" from "{prefix}.one" or "{prefix}.many". */
	private static String count(Wording text, String prefix, int n) {
		return text.get(prefix + ((n == 1) ? ".one" : ".many"), text.format().number(n));
	}

	/** schema.org breadcrumbs, so search results show where a page sits: FutSlot › Kathmandu › New Baneshwor. */
	private String breadcrumbs(List<DirectoryPage.Crumb> crumbs) {
		List<Map<String, Object>> items = new ArrayList<>();
		for (int i = 0; i < crumbs.size(); i++) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("@type", "ListItem");
			item.put("position", i + 1);
			item.put("name", crumbs.get(i).name());
			if (crumbs.get(i).href() != null) {
				item.put("item", this.properties.home() + crumbs.get(i).href());
			}
			items.add(item);
		}
		return write(Map.of("@context", "https://schema.org", "@type", "BreadcrumbList", "itemListElement", items));
	}

	private String write(Object data) {
		// "</" inside the script would end it early
		return this.json.writeValueAsString(data).replace("</", "<\\/");
	}

	private static DirectoryPage.Crumb root(Wording text) {
		return new DirectoryPage.Crumb("FutSlot", text.nepali() ? "/ne/" : "/");
	}

	private static String base(Wording text) {
		return text.nepali() ? SiteLanguage.NEPALI : "";
	}

	/** {@code path} in the other language. */
	private static String other(Wording text, String path) {
		return text.nepali() ? path : SiteLanguage.NEPALI + path;
	}

	private String shareImage() {
		return this.properties.home() + "/site-assets/futslot-share.jpg";
	}

	private String owners() {
		return this.properties.urlFor("owner") + "/";
	}

	private LocalDate today() {
		return LocalDate.now(this.clock.withZone(ZoneId.of("Asia/Kathmandu")));
	}

	/** A venue's page: its details, FutSlot's header and footer around them, and its page in the player app. */
	record VenuePage(SitePage page, DirectoryPage chrome, String app) {
	}

	private record Directory(List<VenueSummaryResponse> venues, Map<String, City> cities) {
	}

	private record City(String name, String slug, List<VenueSummaryResponse> venues, Map<String, Area> areas) {
	}

	private record Area(String name, String slug, List<VenueSummaryResponse> venues) {
	}

}
