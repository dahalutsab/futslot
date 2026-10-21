package com.futslot.site;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.futslot.membership.MembershipPlanResponse;
import com.futslot.membership.MembershipService;
import com.futslot.membership.MembershipType;
import com.futslot.review.RatingSummaryResponse;
import com.futslot.review.ReviewResponse;
import com.futslot.review.ReviewService;
import com.futslot.slot.AvailabilityResponse;
import com.futslot.slot.AvailabilityService;
import com.futslot.slot.SlotStatus;
import com.futslot.tournament.TournamentService;
import com.futslot.tournament.TournamentViews;
import com.futslot.venue.CourtResponse;
import com.futslot.venue.DayHours;
import com.futslot.venue.PhotoResponse;
import com.futslot.venue.PricingRuleResponse;
import com.futslot.venue.VenueResponse;
import com.futslot.venue.VenueService;
import com.futslot.venue.VenueSummaryResponse;

import tools.jackson.databind.json.JsonMapper;

/** Gathers a venue's public information into a {@link SitePage} for a template to render. */
@Service
class SitePageService {

	/** Sunday first, the way the week is counted in Nepal. */
	private static final List<DayOfWeek> WEEK = List.of(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
			DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);

	/** How many days ahead the slot picker offers. */
	private static final int DAYS_SHOWN = 7;

	private final VenueService venues;

	private final AvailabilityService availability;

	private final ReviewService reviews;

	private final MembershipService memberships;

	private final TournamentService tournaments;

	private final JsonMapper json;

	private final Clock clock;

	SitePageService(VenueService venues, AvailabilityService availability, ReviewService reviews,
			MembershipService memberships, TournamentService tournaments, JsonMapper json, Clock clock) {
		this.venues = venues;
		this.availability = availability;
		this.reviews = reviews;
		this.memberships = memberships;
		this.tournaments = tournaments;
		this.json = json;
		this.clock = clock;
	}

	/**
	 * @param canonical the site's own address, without the language
	 * @param linkPrefix what links to other days start with, e.g. "?" on a live site or "?template=turf&" in a
	 * preview, so switching day keeps the same view
	 * @param switchHref this page in the other language
	 */
	SitePage build(String slug, SiteTemplate template, String accent, String headline, String requestedDate,
			String canonical, String linkPrefix, boolean noindex, SiteText text, String switchHref) {
		return build(slug, template, accent, headline, requestedDate, canonical, linkPrefix, noindex, text, switchHref, null,
				null);
	}

	/**
	 * @param bookingBase where the site's booking pages are, in the page's language ("" or "/ne" on the venue's own
	 * address); null in previews, where slots link to a WhatsApp message instead
	 * @param mine link to the signed-in player's bookings, or null
	 */
	SitePage build(String slug, SiteTemplate template, String accent, String headline, String requestedDate,
			String canonical, String linkPrefix, boolean noindex, SiteText text, String switchHref, String bookingBase,
			String mine) {
		SiteFormat format = text.format();
		VenueResponse venue = this.venues.getActiveBySlug(slug);
		ZoneId zone = ZoneId.of(venue.timezone());
		LocalDate today = LocalDate.now(this.clock.withZone(zone));
		LocalDate date = chosenDay(requestedDate, today, venue.policy().maxAdvanceBookingDays());
		String color = (accent != null) ? accent : defaultAccent(template);
		Contact contact = contact(venue);
		List<SitePage.Photo> photos = new ArrayList<>();
		for (PhotoResponse photo : venue.photos()) {
			photos.add(new SitePage.Photo(photo.url(), photo.caption(), (photo.caption() != null) ? photo.caption()
					: text.get("site.photoAlt", venue.name(), format.number(photos.size() + 1))));
		}
		String cover = photos.isEmpty() ? null : photos.getFirst().url();
		SitePage.Pricing pricing = pricing(venue.courts(), text);
		List<SitePage.HoursRow> hours = hours(venue.operatingHours(), today.getDayOfWeek(), text);
		DayHours todays = find(venue.operatingHours(), today.getDayOfWeek());
		String todayHours = (todays != null) ? format.span(todays.openTime(), todays.closeTime()) : null;
		RatingSummaryResponse summary = this.reviews.summary(slug);
		SitePage.Rating rating = null;
		if (summary.reviewCount() > 0 && summary.averageRating() != null) {
			String value = summary.averageRating().setScale(1, RoundingMode.HALF_UP).toPlainString();
			String average = format.number(value);
			rating = new SitePage.Rating(average, summary.reviewCount(), summary.averageRating().setScale(0, RoundingMode.HALF_UP).intValue(),
					text.get("rating.outOf", average), text.get((summary.reviewCount() == 1) ? "rating.reviews.one" : "rating.reviews.many",
							format.number(summary.reviewCount())), value);
		}
		String place = StringUtils.hasText(venue.area()) ? venue.area() + ", " + venue.city() : venue.city();
		String formats = text.list(venue.courts().stream().map(court -> text.get("format." + court.format().name())).distinct().toList());
		LocalTime now = LocalTime.now(this.clock.withZone(zone));
		String openUntil = openUntil(venue.operatingHours(), today.getDayOfWeek(), now, format);
		String openNow = (openUntil != null) ? text.get("open.until", openUntil)
				: opensNext(venue.operatingHours(), today.getDayOfWeek(), now, text);
		SitePage.Venue about = new SitePage.Venue(venue.name(), StringUtils.hasText(headline) ? headline : null,
				venue.description(), place, venue.addressLine(), cover, venue.courts().size(), format.number(venue.courts().size()),
				formats, todayHours, openNow, openUntil != null);
		SitePage.Slots slots = slots(slug, venue, contact, date, today, linkPrefix, bookingBase, mine, text);
		// Late in the evening nothing is left today: unless a day was asked for, show tomorrow instead
		if (requestedDate == null && slots.free() == 0 && venue.policy().maxAdvanceBookingDays() >= 1) {
			slots = slots(slug, venue, contact, today.plusDays(1), today, linkPrefix, bookingBase, mine, text);
		}
		String background = (template == SiteTemplate.FLOODLIGHT) ? "#131715" : "#ffffff";
		List<String> amenities = venue.amenities()
			.stream()
			.map(a -> text.orElse("amenity." + a.code(), a.name()))
			.toList();
		return new SitePage(template.name().toLowerCase(), text.code(), switchHref, color, SiteFormat.inkOn(color),
				SiteFormat.readableOn(color, background),
				seo(venue, about, pricing, rating, contact, canonical, cover, noindex, text), about, contact.page(), photos,
				pricing, slots, hours, amenities, rating, reviews(slug, text), plans(slug, text), tournaments(venue, text),
				format.year(today));
	}

	/** The slug of a venue that's listed; empty once it isn't (a suspended venue has no site). */
	Optional<String> slugOf(UUID venueId) {
		return this.venues.summaries(List.of(venueId)).stream().map(VenueSummaryResponse::slug).findFirst();
	}

	/** The colour each template starts from until the venue picks its own. */
	static String defaultAccent(SiteTemplate template) {
		return (template == SiteTemplate.FLOODLIGHT) ? "#c6f432" : "#15803d";
	}

	private static LocalDate chosenDay(String requested, LocalDate today, int maxAdvanceDays) {
		if (requested == null) {
			return today;
		}
		try {
			LocalDate date = LocalDate.parse(requested);
			return (date.isBefore(today) || date.isAfter(today.plusDays(maxAdvanceDays))) ? today : date;
		}
		catch (DateTimeParseException ex) {
			return today;
		}
	}

	/** When the venue closes if it's open at {@code now}; null if it's closed. Late hours count to the day before. */
	private static String openUntil(List<DayHours> hours, DayOfWeek today, LocalTime now, SiteFormat format) {
		DayHours todays = find(hours, today);
		if (todays != null) {
			boolean overnight = !todays.closeTime().isAfter(todays.openTime());
			if (!now.isBefore(todays.openTime()) && (overnight || now.isBefore(todays.closeTime()))) {
				return format.clock(todays.closeTime());
			}
		}
		DayHours yesterdays = find(hours, today.minus(1));
		if (yesterdays != null && !yesterdays.closeTime().isAfter(yesterdays.openTime()) && now.isBefore(yesterdays.closeTime())) {
			return format.clock(yesterdays.closeTime());
		}
		return null;
	}

	/** "Opens at 6 AM" later today, "Opens tomorrow at 6 AM", or "Closed today". */
	private static String opensNext(List<DayHours> hours, DayOfWeek today, LocalTime now, SiteText text) {
		DayHours todays = find(hours, today);
		if (todays != null && now.isBefore(todays.openTime())) {
			return text.get("open.at", text.format().clock(todays.openTime()));
		}
		DayHours tomorrows = find(hours, today.plus(1));
		return (tomorrows != null) ? text.get("open.tomorrow", text.format().clock(tomorrows.openTime()))
				: text.get("open.closedToday");
	}

	/** A day's hours, or null when the venue is closed that day. */
	private static DayHours find(List<DayHours> hours, DayOfWeek day) {
		return hours.stream()
			.filter(h -> h.day() == day && !Boolean.TRUE.equals(h.closed()) && h.openTime() != null && h.closeTime() != null)
			.findFirst()
			.orElse(null);
	}

	private SitePage.Slots slots(String slug, VenueResponse venue, Contact contact, LocalDate date, LocalDate today,
			String linkPrefix, String bookingBase, String mine, SiteText text) {
		SiteFormat format = text.format();
		AvailabilityResponse open = this.availability.forPlayers(slug, date);
		List<SitePage.CourtSlots> courts = new ArrayList<>();
		List<Map.Entry<java.time.OffsetDateTime, SitePage.BoardRow>> board = new ArrayList<>();
		int free = 0;
		for (AvailabilityResponse.CourtSlots court : open.courts()) {
			List<SitePage.Slot> slots = court.slots()
				.stream()
				.filter(slot -> slot.status() == SlotStatus.AVAILABLE)
				.map(slot -> {
					String time = format.clock(slot.startsAt().toLocalTime());
					String price = format.rupees(slot.price());
					String message = text.get("whatsapp.book", court.name(), venue.name(), format.day(date), time, price);
					String book = (bookingBase != null)
							? bookingBase + "/book?court=" + court.courtId() + "&start=" + URLEncoder.encode(slot.startsAt().toString(), StandardCharsets.UTF_8)
							: contact.bookingLink(message);
					board.add(Map.entry(slot.startsAt(), new SitePage.BoardRow(time, court.name(), price, book)));
					return new SitePage.Slot(time, price, book);
				})
				.toList();
			free += slots.size();
			courts.add(new SitePage.CourtSlots(court.name(), slots));
		}
		int span = Math.min(DAYS_SHOWN, venue.policy().maxAdvanceBookingDays() + 1);
		List<SitePage.Day> days = new ArrayList<>();
		for (int i = 0; i < span; i++) {
			LocalDate day = today.plusDays(i);
			String label = (i == 0) ? text.get("day.today") : (i == 1) ? text.get("day.tomorrow") : format.dayOfWeek(day);
			days.add(new SitePage.Day(label, day.toString(), linkPrefix + "date=" + day + "#slots", day.equals(date)));
		}
		String relative = date.equals(today) ? "today" : date.equals(today.plusDays(1)) ? "tomorrow" : null;
		String dayLabel = (relative != null) ? text.get("day." + relative) : format.day(date);
		List<SitePage.BoardRow> rows = board.stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).toList();
		String count = text.get((free == 1) ? "slots.one" : "slots.many", format.number(free));
		return new SitePage.Slots(dayLabel, relative, days, courts, rows, free, count, bookingBase != null, mine);
	}

	private static SitePage.Pricing pricing(List<CourtResponse> courts, SiteText text) {
		BigDecimal lowest = null;
		List<SitePage.Court> list = new ArrayList<>();
		for (CourtResponse court : courts) {
			List<PricingRuleResponse> active = court.pricingRules()
				.stream()
				.filter(PricingRuleResponse::active)
				.sorted(Comparator.comparing((PricingRuleResponse rule) -> rule.dayOfWeek() != null)
					.thenComparing(rule -> rule.dayOfWeek() == null ? 0 : WEEK.indexOf(rule.dayOfWeek()))
					.thenComparing(PricingRuleResponse::startTime))
				.toList();
			List<SitePage.Price> prices = joinAcrossMidnight(active, text);
			for (PricingRuleResponse rule : court.pricingRules()) {
				if (rule.active() && (lowest == null || rule.price().compareTo(lowest) < 0)) {
					lowest = rule.price();
				}
			}
			String details = String.join(" · ", text.get("format." + court.format().name()),
					text.get("surface." + court.surface().name()), text.get(court.indoor() ? "court.indoor" : "court.outdoor"),
					text.get("court.minutes", text.format().number(court.slotDurationMinutes())));
			list.add(new SitePage.Court(court.name(), details, prices));
		}
		return new SitePage.Pricing(lowest == null ? null : text.format().rupees(lowest), list);
	}

	/**
	 * Prices as the venue thinks of them. A late-night price is stored as two rules split at midnight
	 * (10 PM – 12 AM and 12 AM – 1 AM, same day, label and price); they read as one: 10 PM – 1 AM.
	 */
	private static List<SitePage.Price> joinAcrossMidnight(List<PricingRuleResponse> rules, SiteText text) {
		// Pair each "to midnight" rule with its "from midnight" continuation first, so either order works
		Map<PricingRuleResponse, PricingRuleResponse> continuations = new LinkedHashMap<>();
		for (PricingRuleResponse rule : rules) {
			if (rule.endTime().getHour() == 23 && rule.endTime().getMinute() == 59) {
				rules.stream()
					.filter(other -> other != rule && !continuations.containsValue(other) && other.startTime().equals(LocalTime.MIDNIGHT)
							&& Objects.equals(other.dayOfWeek(), rule.dayOfWeek()) && other.label() == rule.label()
							&& other.price().compareTo(rule.price()) == 0)
					.findFirst()
					.ifPresent(after -> continuations.put(rule, after));
			}
		}
		List<SitePage.Price> prices = new ArrayList<>();
		for (PricingRuleResponse rule : rules) {
			if (continuations.containsValue(rule)) {
				continue;
			}
			LocalTime end = continuations.containsKey(rule) ? continuations.get(rule).endTime() : rule.endTime();
			String days = (rule.dayOfWeek() == null) ? text.get("price.everyDay")
					: text.get("price.weekly", text.format().weekday(rule.dayOfWeek()));
			prices.add(new SitePage.Price(text.orElse("price." + rule.label().name(), rule.label().name()),
					text.get("price.when", days, text.format().span(rule.startTime(), end)), text.format().rupees(rule.price())));
		}
		return prices;
	}

	private static List<SitePage.HoursRow> hours(List<DayHours> hours, DayOfWeek today, SiteText text) {
		return WEEK.stream().map(day -> {
			DayHours saved = hours.stream().filter(h -> h.day() == day).findFirst().orElse(null);
			String open = (saved == null || Boolean.TRUE.equals(saved.closed()) || saved.openTime() == null) ? text.get("hours.closed")
					: text.format().span(saved.openTime(), saved.closeTime());
			return new SitePage.HoursRow(text.format().weekday(day), open, day == today);
		}).toList();
	}

	private List<SitePage.Review> reviews(String slug, SiteText text) {
		List<ReviewResponse> latest = this.reviews.published(slug, PageRequest.of(0, 6, Sort.by(Sort.Order.desc("createdAt"))))
			.content();
		return latest.stream()
			.map(r -> new SitePage.Review(r.reviewerName(), r.rating(), text.get("rating.outOf", text.format().number(r.rating())),
					r.comment(), text.get("review.played", text.format().day(r.playedOn())), r.ownerReply()))
			.toList();
	}

	private List<SitePage.Plan> plans(String slug, SiteText text) {
		return this.memberships.plansOnSale(slug).stream().map(plan -> plan(plan, text)).toList();
	}

	private static SitePage.Plan plan(MembershipPlanResponse plan, SiteText text) {
		SiteFormat format = text.format();
		String valid = text.get("plan.valid", format.number(plan.validityDays()));
		String summary = (plan.type() == MembershipType.GAME_PACK) ? text.get("plan.games", format.number(plan.gamesIncluded()), valid)
				: (plan.gamesIncluded() != null)
						? text.get("plan.offUpTo", format.number(plan.discountPercent()), format.number(plan.gamesIncluded()), valid)
						: text.get("plan.offAll", format.number(plan.discountPercent()), valid);
		return new SitePage.Plan(plan.name(), summary, format.rupees(plan.price()), plan.description());
	}

	private List<SitePage.Tournament> tournaments(VenueResponse venue, SiteText text) {
		return this.tournaments.upcomingAtVenue(venue.id()).stream().map(t -> tournament(t, text)).toList();
	}

	private static SitePage.Tournament tournament(TournamentViews.Summary t, SiteText text) {
		SiteFormat format = text.format();
		String dates = t.startsOn().equals(t.endsOn()) ? format.day(t.startsOn())
				: format.day(t.startsOn()) + " – " + format.day(t.endsOn());
		String entry = (t.entryFee() == null || t.entryFee().signum() == 0) ? text.get("tournament.free")
				: text.get("tournament.perTeam", format.rupees(t.entryFee()));
		return new SitePage.Tournament(t.name(), dates, entry,
				text.get("tournament.spots", format.number(t.teamsIn()), format.number(t.maxTeams())),
				text.get("tournament." + t.format().name()));
	}

	private SitePage.Seo seo(VenueResponse venue, SitePage.Venue about, SitePage.Pricing pricing, SitePage.Rating rating,
			Contact contact, String canonical, String cover, boolean noindex, SiteText text) {
		String title = text.get("seo.title", venue.name(), about.place());
		List<String> sentences = new ArrayList<>();
		String courts = text.get((about.courtCount() == 1) ? "seo.courts.one" : "seo.courts.many", about.courts());
		sentences.add(text.get("seo.intro", venue.name(), courts, about.place(),
				(pricing.from() != null) ? text.get("seo.from", pricing.from()) : ""));
		if (about.todayHours() != null) {
			sentences.add(text.get("seo.openToday", about.todayHours()));
		}
		sentences.add(text.get(contact.page().phone() != null ? "seo.bookByPhone" : "seo.book"));
		String image = (cover != null) ? canonical + cover : null;
		String english = noindex ? null : canonical + "/";
		String nepali = noindex ? null : canonical + SiteLanguage.NEPALI + "/";
		return new SitePage.Seo(title, String.join(" ", sentences), text.nepali() ? canonical + SiteLanguage.NEPALI + "/" : canonical + "/",
				english, nepali, image, jsonLd(venue, pricing, rating, contact, canonical, image, text), noindex);
	}

	/** schema.org data search engines use for opening hours, location, prices and rating. */
	private String jsonLd(VenueResponse venue, SitePage.Pricing pricing, SitePage.Rating rating, Contact contact,
			String canonical, String image, SiteText text) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("@context", "https://schema.org");
		data.put("@type", "SportsActivityLocation");
		data.put("name", venue.name());
		if (StringUtils.hasText(venue.description())) {
			data.put("description", venue.description());
		}
		data.put("url", canonical);
		if (image != null) {
			data.put("image", image);
		}
		if (contact.page().phone() != null) {
			data.put("telephone", venue.phone());
		}
		Map<String, Object> address = new LinkedHashMap<>();
		address.put("@type", "PostalAddress");
		address.put("streetAddress", venue.addressLine());
		address.put("addressLocality", StringUtils.hasText(venue.area()) ? venue.area() : venue.city());
		address.put("addressRegion", venue.city());
		address.put("addressCountry", "NP");
		data.put("address", address);
		if (venue.latitude() != null && venue.longitude() != null) {
			data.put("geo", Map.of("@type", "GeoCoordinates", "latitude", venue.latitude(), "longitude", venue.longitude()));
		}
		List<Map<String, Object>> opening = venue.operatingHours()
			.stream()
			.filter(h -> !Boolean.TRUE.equals(h.closed()) && h.openTime() != null)
			.sorted(Comparator.comparing(h -> WEEK.indexOf(h.day())))
			.map(h -> Map.<String, Object>of("@type", "OpeningHoursSpecification", "dayOfWeek",
					"https://schema.org/" + SiteFormat.ENGLISH.weekday(h.day()), "opens", hhmm(h.openTime()), "closes", hhmm(h.closeTime())))
			.toList();
		if (!opening.isEmpty()) {
			data.put("openingHoursSpecification", opening);
		}
		if (pricing.from() != null) {
			data.put("priceRange", text.get("seo.priceRange", pricing.from()));
		}
		if (rating != null) {
			data.put("aggregateRating", Map.of("@type", "AggregateRating", "ratingValue", rating.value(), "reviewCount",
					rating.count(), "bestRating", 5));
		}
		if (!venue.amenities().isEmpty()) {
			data.put("amenityFeature", venue.amenities()
				.stream()
				.map(a -> Map.of("@type", "LocationFeatureSpecification", "name", a.name(), "value", true))
				.toList());
		}
		// "</" inside the script would end it early
		return this.json.writeValueAsString(data).replace("</", "<\\/");
	}

	private static String hhmm(LocalTime time) {
		return "%02d:%02d".formatted(time.getHour(), time.getMinute());
	}

	private static Contact contact(VenueResponse venue) {
		String query = (venue.latitude() != null && venue.longitude() != null)
				? venue.latitude().toPlainString() + "," + venue.longitude().toPlainString()
				: String.join(", ", venue.name(), venue.addressLine(), venue.city());
		String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
		String mapEmbed = "https://maps.google.com/maps?q=" + encoded + "&z=16&output=embed";
		String directions = "https://www.google.com/maps/dir/?api=1&destination=" + encoded;
		String phone = venue.phone();
		if (!StringUtils.hasText(phone)) {
			return new Contact(new SitePage.Contact(null, null, null, mapEmbed, directions), null);
		}
		String digits = phone.replaceAll("\\D", "");
		String shown = phone.startsWith("+977") && phone.length() == 14 ? "+977 " + phone.substring(4) : phone;
		String whatsapp = "https://wa.me/" + digits;
		return new Contact(new SitePage.Contact(shown, "tel:" + phone, whatsapp, mapEmbed, directions), whatsapp);
	}

	/** The page's contact details, and the WhatsApp chat a booking request opens (null without a phone). */
	private record Contact(SitePage.Contact page, String whatsapp) {

		String bookingLink(String message) {
			if (this.whatsapp == null) {
				return null;
			}
			return this.whatsapp + "?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8).replace("+", "%20");
		}

	}

}
