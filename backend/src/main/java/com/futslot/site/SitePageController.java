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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Venue websites. Live sites are reached at their own address ({@link SiteHostFilter} forwards those requests
 * here); {@code /sites/{subdomain}} serves the same page directly, and {@code /site-preview/{slug}} shows any
 * template before it's published, hidden from search engines.
 */
@Controller
class SitePageController {

	private final VenueSiteRepository sites;

	private final SitePageService pages;

	private final SiteProperties properties;

	SitePageController(VenueSiteRepository sites, SitePageService pages, SiteProperties properties) {
		this.sites = sites;
		this.pages = pages;
		this.properties = properties;
	}

	@GetMapping("/sites/{subdomain}")
	String site(@PathVariable String subdomain, @RequestParam(required = false) String date, Model model,
			HttpServletResponse response) {
		VenueSite site = published(subdomain);
		String slug = this.pages.slugOf(site.getVenueId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		model.addAttribute("page", this.pages.build(slug, site.getTemplate(), site.getAccentColor(), site.getHeadline(), date,
				canonical(site), "?", false));
		response.setHeader("Cache-Control", CacheControl.maxAge(Duration.ofMinutes(2)).cachePublic().getHeaderValue());
		return "sites/" + site.getTemplate().name().toLowerCase(Locale.ROOT);
	}

	@GetMapping("/site-preview/{slug}")
	String preview(@PathVariable String slug, @RequestParam(defaultValue = "TURF") String template,
			@RequestParam(required = false) String accent, @RequestParam(required = false) String headline,
			@RequestParam(required = false) String date, Model model) {
		SiteTemplate chosen = parseTemplate(template);
		String color = (accent != null && accent.matches("#?[0-9a-fA-F]{6}")) ? "#" + accent.replace("#", "").toLowerCase(Locale.ROOT) : null;
		String prefix = "?template=" + chosen.name() + (color != null ? "&accent=" + color.substring(1) : "") + "&";
		model.addAttribute("page", this.pages.build(slug, chosen, color, headline, date, this.properties.urlFor(slug), prefix, true));
		return "sites/" + chosen.name().toLowerCase(Locale.ROOT);
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
		return """
				<?xml version="1.0" encoding="UTF-8"?>
				<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
				  <url><loc>%s/</loc><lastmod>%s</lastmod><changefreq>daily</changefreq></url>
				</urlset>
				""".formatted(canonical(site), site.getUpdatedAt().toString().substring(0, 10));
	}

	/**
	 * Asked by the web server before it gets an HTTPS certificate for a host: only hosts of live sites qualify,
	 * so nobody can make FutSlot request certificates for domains that aren't venues'.
	 */
	@GetMapping("/internal/sites/domain-check")
	ResponseEntity<Void> domainCheck(@RequestParam String domain) {
		String host = domain.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
		String suffix = "." + this.properties.baseDomain();
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
