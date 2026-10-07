package com.futslot.site;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Sends requests for a venue's own address (dhuku.futslot.com, or a custom domain) to its site pages. Runs
 * before security: site pages are public, and anything else on those hosts other than photos and the site's
 * stylesheets doesn't exist there.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class SiteHostFilter extends OncePerRequestFilter {

	private final VenueSiteRepository sites;

	private final SiteProperties properties;

	SiteHostFilter(VenueSiteRepository sites, SiteProperties properties) {
		this.sites = sites;
		this.properties = properties;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Optional<String> subdomain = siteFor(request.getServerName().toLowerCase(Locale.ROOT));
		if (subdomain.isEmpty()) {
			chain.doFilter(request, response);
			return;
		}
		String path = request.getRequestURI();
		if (path.startsWith("/media/") || path.startsWith("/site-assets/")) {
			chain.doFilter(request, response);
			return;
		}
		String target = switch (path) {
			case "/", "" -> "/sites/" + subdomain.get();
			case "/robots.txt" -> "/sites/" + subdomain.get() + "/robots.txt";
			case "/sitemap.xml" -> "/sites/" + subdomain.get() + "/sitemap.xml";
			default -> null;
		};
		if (target == null || !"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		request.getRequestDispatcher(target).forward(request, response);
	}

	/** The site this host belongs to, if any: a subdomain of the base domain, or a custom domain. */
	private Optional<String> siteFor(String host) {
		String base = this.properties.baseDomain();
		if (host.equals(base) || host.equals("www." + base)) {
			return Optional.empty();
		}
		if (host.endsWith("." + base)) {
			String subdomain = host.substring(0, host.length() - base.length() - 1);
			return (subdomain.contains(".") || SiteService.RESERVED.contains(subdomain)) ? Optional.empty() : Optional.of(subdomain);
		}
		if (!host.contains(".") || host.chars().allMatch(c -> Character.isDigit(c) || c == '.')) {
			return Optional.empty();
		}
		return this.sites.findByCustomDomain(host.replaceFirst("^www\\.", "")).map(VenueSite::getSubdomain);
	}

}
