package com.futslot.site;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import com.futslot.common.Languages;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Which language a site page is in. English pages are at the site's own address, Nepali ones beneath /ne
 * (dhuku.futslot.com/ne/, /sites/dhuku/ne/book...), so each has an address of its own that caches and search
 * engines can tell apart, and every link a page makes from its base stays in its language.
 */
@Component
class SiteLanguage {

	/** What a site's Nepali addresses start with, after the site's own base. */
	static final String NEPALI = "/ne";

	private static final Pattern NEPALI_PAGE = Pattern.compile("/sites/[^/]+" + NEPALI + "(/.*)?");

	private final MessageSource messages;

	SiteLanguage(MessageSource messages) {
		this.messages = messages;
	}

	/** The language of the page being asked for, from its address. */
	SiteText of(HttpServletRequest request) {
		return of(nepali(request) ? Languages.NEPALI : Languages.ENGLISH, request);
	}

	/** {@code locale} for this page: its wording, and Thymeleaf's for the templates' #{...} messages. */
	SiteText of(Locale locale, HttpServletRequest request) {
		request.setAttribute(Languages.CHOSEN, locale);
		return new SiteText(this.messages, locale);
	}

	/**
	 * What the site's links start with in a language: "" or "/ne" on the venue's own address (the request was
	 * forwarded here by {@link SiteHostFilter}), "/sites/{subdomain}" or "/sites/{subdomain}/ne" otherwise.
	 */
	static String base(HttpServletRequest request, String subdomain, boolean nepali) {
		String site = (request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) != null) ? "" : "/sites/" + subdomain;
		return site + (nepali ? NEPALI : "");
	}

	/** The site's front page from its {@code base}: "/" on the venue's own address, the base itself otherwise. */
	static String home(String base) {
		return base.isEmpty() ? "/" : base;
	}

	private static boolean nepali(HttpServletRequest request) {
		return NEPALI_PAGE.matcher(request.getRequestURI()).matches();
	}

}
