package com.futslot.site;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.futslot.common.Languages;
import com.futslot.common.Wording;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Venue websites. Live sites are reached at their own address ({@link SiteHostFilter} forwards those requests
 * here); {@code /sites/{subdomain}} serves the same page directly, and {@code /site-preview/{slug}} shows any
 * template before it's published, hidden from search engines. Each page is in English, and in Nepali beneath
 * {@code /ne} (see {@link SiteLanguage}).
 */
@Controller
class SitePageController {

	private final VenueSiteRepository sites;

	private final SitePageService pages;

	private final SiteProperties properties;

	private final SiteSession session;

	private final SiteLanguage language;

	SitePageController(VenueSiteRepository sites, SitePageService pages, SiteProperties properties, SiteSession session,
			SiteLanguage language) {
		this.sites = sites;
		this.pages = pages;
		this.properties = properties;
		this.session = session;
		this.language = language;
	}

	@GetMapping({ "/sites/{subdomain}", "/sites/{subdomain}" + SiteLanguage.NEPALI })
	String site(@PathVariable String subdomain, @RequestParam(required = false) String date, Model model,
			HttpServletRequest request, HttpServletResponse response) {
		VenueSite site = published(subdomain);
		String slug = this.pages.slugOf(site.getVenueId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		Wording text = this.language.of(request);
		// On the venue's own address the booking pages are at /book; at /sites/{subdomain} they're beneath it
		String base = SiteLanguage.base(request, site.getSubdomain(), text.nepali());
		String other = SiteLanguage.home(SiteLanguage.base(request, site.getSubdomain(), !text.nepali()))
				+ ((date != null && date.matches("\\d{4}-\\d{2}-\\d{2}")) ? "?date=" + date : "");
		String mine = this.session.player(request).isPresent() ? base + "/bookings" : null;
		model.addAttribute("page", this.pages.build(slug, site.getTemplate(), site.getAccentColor(), site.getHeadline(), date,
				SitePageService.Addresses.site(canonical(site)), "?", !this.properties.searchIndexing(), text, other, base, mine));
		if (mine != null) {
			// Personal (it links to their bookings), so not for shared caches
			response.setHeader("Cache-Control", "private, no-cache");
			return "sites/" + site.getTemplate().name().toLowerCase(Locale.ROOT);
		}
		response.setHeader("Cache-Control", CacheControl.maxAge(Duration.ofMinutes(2)).cachePublic().getHeaderValue());
		return "sites/" + site.getTemplate().name().toLowerCase(Locale.ROOT);
	}

	@GetMapping("/site-preview/{slug}")
	String preview(@PathVariable String slug, @RequestParam(defaultValue = "TURF") String template,
			@RequestParam(required = false) String accent, @RequestParam(required = false) String headline,
			@RequestParam(required = false) String date, @RequestParam(required = false) String lang, Model model,
			HttpServletRequest request) {
		SiteTemplate chosen = parseTemplate(template);
		String color = (accent != null && accent.matches("#?[0-9a-fA-F]{6}")) ? "#" + accent.replace("#", "").toLowerCase(Locale.ROOT) : null;
		Wording text = this.language.of(Languages.of(lang), request);
		String view = "?template=" + chosen.name() + (color != null ? "&accent=" + color.substring(1) : "");
		String prefix = view + (text.nepali() ? "&lang=ne" : "") + "&";
		model.addAttribute("page", this.pages.build(slug, chosen, color, headline, date,
				SitePageService.Addresses.site(this.properties.urlFor(slug)), prefix, true,
				text, view + (text.nepali() ? "" : "&lang=ne")));
		return "sites/" + chosen.name().toLowerCase(Locale.ROOT);
	}

	/** FutSlot's own address, for the "Website by FutSlot" link at the foot of every site. */
	@ModelAttribute("futslotHome")
	String futslotHome() {
		return this.properties.home() + "/";
	}

	@GetMapping(value = "/sites/{subdomain}/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
	@ResponseBody
	String robots(@PathVariable String subdomain) {
		VenueSite site = published(subdomain);
		return "User-agent: *\nAllow: /\nSitemap: " + canonical(site) + "/sitemap.xml\n";
	}

	@GetMapping(value = "/sites/{subdomain}/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
	@ResponseBody
	String sitemap(@PathVariable String subdomain) {
		VenueSite site = published(subdomain);
		String english = canonical(site) + "/";
		String nepali = canonical(site) + SiteLanguage.NEPALI + "/";
		String alternates = """
				    <xhtml:link rel="alternate" hreflang="en" href="%s"/>
				    <xhtml:link rel="alternate" hreflang="ne" href="%s"/>
				""".formatted(english, nepali);
		String url = """
				  <url>
				    <loc>%s</loc><lastmod>%s</lastmod><changefreq>daily</changefreq>
				%s  </url>
				""";
		String updated = site.getUpdatedAt().toString().substring(0, 10);
		return """
				<?xml version="1.0" encoding="UTF-8"?>
				<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">
				%s%s</urlset>
				""".formatted(url.formatted(english, updated, alternates), url.formatted(nepali, updated, alternates));
	}

	/**
	 * Asked by the web server before it gets an HTTPS certificate for a host: only hosts of live sites and
	 * FutSlot's own apps qualify, so nobody can make FutSlot request certificates for domains that aren't venues'.
	 */
	@GetMapping("/internal/sites/domain-check")
	ResponseEntity<Void> domainCheck(@RequestParam String domain) {
		String host = domain.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
		String base = this.properties.baseDomain();
		if (host.equals(base) || host.equals("owner." + base) || host.equals("admin." + base)) {
			return ResponseEntity.ok().build();
		}
		String suffix = "." + base;
		Optional<VenueSite> site = host.endsWith(suffix)
				? this.sites.findBySubdomain(host.substring(0, host.length() - suffix.length()))
				: this.sites.findByCustomDomain(host);
		return site.filter(VenueSite::isPublished).isPresent() ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
	}

	private VenueSite published(String subdomain) {
		return this.sites.findBySubdomain(subdomain.toLowerCase(Locale.ROOT))
			.filter(VenueSite::isPublished)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	/** The site's own address: its custom domain if it has one, otherwise its FutSlot address. */
	private String canonical(VenueSite site) {
		return StringUtils.hasText(site.getCustomDomain()) ? "https://" + site.getCustomDomain()
				: this.properties.urlFor(site.getSubdomain());
	}

	private static SiteTemplate parseTemplate(String text) {
		try {
			return SiteTemplate.valueOf(text.toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			return SiteTemplate.TURF;
		}
	}

}
