package com.futslot.site;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import com.futslot.common.Languages;

import jakarta.servlet.http.HttpServletRequest;

/**
 * A page that isn't there (a city with no venues, a venue that left, a venue website that's gone) is a page people
 * see, not the API's JSON: FutSlot's "not found" page, in the language of the address that was asked for. Ahead
 * of the API's own error handling, for the page controllers only.
 */
@ControllerAdvice(assignableTypes = { DirectoryController.class, SitePageController.class, SiteBookingController.class })
@Order(Ordered.HIGHEST_PRECEDENCE)
class PageNotFound {

	private static final Pattern NEPALI = Pattern.compile("^(/sites/[^/]+)?/ne(/.*)?$");

	private final SiteLanguage language;

	private final SiteProperties properties;

	PageNotFound(SiteLanguage language, SiteProperties properties) {
		this.language = language;
		this.properties = properties;
	}

	@ExceptionHandler(ResponseStatusException.class)
	ModelAndView notFound(ResponseStatusException ex, HttpServletRequest request) {
		if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
			throw ex;
		}
		boolean nepali = NEPALI.matcher(request.getRequestURI()).matches();
		Locale locale = nepali ? Languages.NEPALI : Languages.ENGLISH;
		this.language.of(locale, request);
		ModelAndView page = new ModelAndView("error/404", HttpStatus.NOT_FOUND);
		page.addObject("lang", locale.getLanguage());
		page.addObject("futslotHome", this.properties.home() + "/");
		return page;
	}

}
