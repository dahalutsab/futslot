package com.futslot.site;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.futslot.common.ApiException;
import com.futslot.venue.Venue;
import com.futslot.venue.VenueAccess;
import com.futslot.venue.VenuePermission;
import com.futslot.venue.VenueStatus;

/** Setting up and publishing a venue's website. */
@Service
class SiteService {

	private static final Pattern SUBDOMAIN = Pattern.compile("[a-z0-9]([a-z0-9-]{1,38}[a-z0-9])");

	private static final Pattern DOMAIN = Pattern
		.compile("(?=.{4,253}$)([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}");

	private static final Pattern COLOR = Pattern.compile("#[0-9a-f]{6}");

	/** Addresses FutSlot keeps for itself. */
	static final Set<String> RESERVED = Set.of("www", "api", "app", "admin", "dashboard", "owner", "mail", "email",
			"static", "assets", "media", "cdn", "futslot", "help", "support", "blog", "status", "docs", "sites", "site",
			"login", "auth", "book", "booking", "pay", "test", "dev", "staging");

	private final VenueSiteRepository sites;

	private final VenueAccess access;

	private final SiteProperties properties;

	SiteService(VenueSiteRepository sites, VenueAccess access, SiteProperties properties) {
		this.sites = sites;
		this.access = access;
		this.properties = properties;
	}

	@Transactional(readOnly = true)
	SiteSettings get(UUID userId, UUID venueId) {
		Venue venue = this.access.require(userId, venueId, VenuePermission.MANAGE_VENUE);
		return this.sites.findById(venueId)
			.map(site -> view(venue, site, true))
			.orElseGet(() -> view(venue, new VenueSite(venueId, SiteTemplate.TURF, suggestSubdomain(venue)), false));
	}

	@Transactional
	SiteSettings save(UUID userId, UUID venueId, SiteRequest request) {
		Venue venue = this.access.require(userId, venueId, VenuePermission.MANAGE_VENUE);
		String subdomain = subdomain(request.subdomain());
		String problem = subdomainProblem(subdomain, venueId);
		if (problem != null) {
			throw badRequest("SUBDOMAIN_UNAVAILABLE", problem);
		}
		String domain = customDomain(request.customDomain());
		if (domain != null && this.sites.existsByCustomDomainAndVenueIdNot(domain, venueId)) {
			throw badRequest("DOMAIN_TAKEN", domain + " is already used by another venue");
		}
		String color = StringUtils.hasText(request.accentColor()) ? request.accentColor().strip().toLowerCase(Locale.ROOT) : null;
		if (color != null && !COLOR.matcher(color).matches()) {
			throw badRequest("INVALID_COLOR", "Use a colour like #16a34a");
		}
		String headline = StringUtils.hasText(request.headline()) ? request.headline().strip() : null;
		VenueSite site = this.sites.findById(venueId)
			.orElseGet(() -> new VenueSite(venueId, request.template(), subdomain));
		site.update(request.template(), subdomain, domain, color, headline);
		return view(venue, this.sites.save(site), true);
	}

	@Transactional
	SiteSettings publish(UUID userId, UUID venueId, boolean published) {
		Venue venue = this.access.require(userId, venueId, VenuePermission.MANAGE_VENUE);
		VenueSite site = this.sites.findById(venueId)
			.orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "SITE_NOT_SET_UP", "Choose a template and address first"));
		if (published && venue.getStatus() != VenueStatus.ACTIVE) {
			throw new ApiException(HttpStatus.CONFLICT, "VENUE_NOT_LIVE",
					"Your website can go live once FutSlot has approved the venue");
		}
		site.setPublished(published);
		return view(venue, site, true);
	}

	/** Null when the address can be used by this venue, otherwise why not. */
	@Transactional(readOnly = true)
	String subdomainProblem(String subdomain, UUID venueId) {
		if (!SUBDOMAIN.matcher(subdomain).matches()) {
			return "Use 3 to 40 lowercase letters, digits or hyphens, starting and ending with a letter or digit";
		}
		if (RESERVED.contains(subdomain)) {
			return subdomain + " is reserved by FutSlot";
		}
		if (this.sites.existsBySubdomainAndVenueIdNot(subdomain, venueId)) {
			return subdomain + " is taken by another venue";
		}
		return null;
	}

	static String subdomain(String text) {
		return text.strip().toLowerCase(Locale.ROOT);
	}

	/** "https://www.DhukuFutsal.com/" becomes "dhukufutsal.com"; blank means none. */
	String customDomain(String text) {
		if (!StringUtils.hasText(text)) {
			return null;
		}
		String domain = text.strip().toLowerCase(Locale.ROOT).replaceFirst("^https?://", "").replaceFirst("/.*$", "")
			.replaceFirst("^www\\.", "");
		if (!DOMAIN.matcher(domain).matches()) {
			throw badRequest("INVALID_DOMAIN", "Enter a domain like dhukufutsal.com");
		}
		if (domain.equals(this.properties.baseDomain()) || domain.endsWith("." + this.properties.baseDomain())) {
			throw badRequest("INVALID_DOMAIN", "That's a FutSlot address; set it as your site address instead");
		}
		return domain;
	}

	private SiteSettings view(Venue venue, VenueSite site, boolean saved) {
		return new SiteSettings(venue.getId(), saved, site.getTemplate(), site.getSubdomain(), site.getCustomDomain(),
				site.getAccentColor(), site.getHeadline(), site.isPublished(), this.properties.urlFor(site.getSubdomain()),
				"/site-preview/" + venue.getSlug(), venue.getStatus() == VenueStatus.ACTIVE, this.properties.dnsTarget());
	}

	/** The venue's slug if it's free, otherwise the slug with a number. */
	private String suggestSubdomain(Venue venue) {
		String base = venue.getSlug().length() > 40 ? venue.getSlug().substring(0, 40).replaceAll("-+$", "") : venue.getSlug();
		String candidate = base;
		for (int n = 2; subdomainProblem(candidate, venue.getId()) != null && n < 100; n++) {
			candidate = base + "-" + n;
		}
		return candidate;
	}

	private static ApiException badRequest(String code, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, code, message);
	}

}
