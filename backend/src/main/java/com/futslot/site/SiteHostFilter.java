package com.futslot.site;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

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
 * stylesheets doesn't exist there. The same pages in Nepali are beneath /ne (see {@link SiteLanguage}).
 * <p>
 * A proxy in front that can't pass the venue's address on (Vercel's rewrites don't) names the site at the start
 * of the path instead: /site-host/dhuku/book is a visit to dhuku's address for /book.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class SiteHostFilter extends OncePerRequestFilter {

	static final String PROXIED = "/site-host/";

	private static final Pattern SUBDOMAIN = Pattern.compile("[a-z0-9][a-z0-9-]*");

	private static final Pattern BOOKING = Pattern.compile(
			"/book(/password|/code|/verify|/confirm)?|/bookings(/[0-9a-f-]{36}(/pay)?)?|/deposit(/withdraw|/withdrawals/[0-9a-f-]{36}/cancel)?|/sign-out");

	private final VenueSiteRepository sites;

	private final SiteProperties properties;

	SiteHostFilter(VenueSiteRepository sites, SiteProperties properties) {
		this.sites = sites;
		this.properties = properties;
	}

	/** Health checks belong to no site, and must not touch the database (a sleeping one would be woken). */
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return request.getRequestURI().startsWith("/actuator/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String path = request.getRequestURI();
		Optional<String> subdomain;
		if (path.startsWith(PROXIED)) {
			int slash = path.indexOf('/', PROXIED.length());
			String named = path.substring(PROXIED.length(), (slash < 0) ? path.length() : slash).toLowerCase(Locale.ROOT);
			if (!SUBDOMAIN.matcher(named).matches()) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			subdomain = Optional.of(named);
			path = (slash < 0) ? "/" : path.substring(slash);
		}
		else {
			subdomain = siteFor(request.getServerName().toLowerCase(Locale.ROOT));
		}
		if (subdomain.isEmpty()) {
			chain.doFilter(request, response);
			return;
		}
		if (path.startsWith("/media/") || path.startsWith("/site-assets/")) {
			if (path.equals(request.getRequestURI())) {
				chain.doFilter(request, response);
			}
			else {
				request.getRequestDispatcher(path).forward(request, response);
			}
			return;
		}
		String site = "/sites/" + subdomain.get();
		boolean english = !path.equals(SiteLanguage.NEPALI) && !path.startsWith(SiteLanguage.NEPALI + "/");
		if (!english) {
			site += SiteLanguage.NEPALI;
			path = path.substring(SiteLanguage.NEPALI.length());
		}
		boolean reading = "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
		String target = null;
		if (reading && (path.isEmpty() || path.equals("/") || (english && (path.equals("/robots.txt") || path.equals("/sitemap.xml"))))) {
			target = site + (path.length() > 1 ? path : "");
		}
		// Booking: the slot, signing in and confirming, the booking itself and the player's deposit (pages and their forms)
		else if (BOOKING.matcher(path).matches()) {
			target = site + path;
		}
		if (target == null) {
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
