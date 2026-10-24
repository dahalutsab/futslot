package com.futslot.site;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.futslot.common.Languages;
import com.futslot.common.Wording;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * FutSlot's own public pages, on its own address (see {@link DirectoryService}); the web server sends these paths
 * here and everything else to the player app. Nepali pages are the same paths beneath /ne.
 */
@Controller
class DirectoryController {

	private static final String CACHE = CacheControl.maxAge(Duration.ofMinutes(2)).cachePublic().getHeaderValue();

	private final DirectoryService directory;

	private final SiteLanguage language;

	DirectoryController(DirectoryService directory, SiteLanguage language) {
		this.directory = directory;
		this.language = language;
	}

	@GetMapping({ "/", "/ne", "/ne/" })
	String home(Model model, HttpServletRequest request, HttpServletResponse response) {
		model.addAttribute("dir", this.directory.home(wording(request)));
		response.setHeader("Cache-Control", CACHE);
		return "futslot/home";
	}

	@GetMapping({ "/futsal/{city}", "/ne/futsal/{city}" })
	String city(@PathVariable String city, Model model, HttpServletRequest request, HttpServletResponse response) {
		model.addAttribute("dir", this.directory.city(city, wording(request)).orElseThrow(DirectoryController::notFound));
		response.setHeader("Cache-Control", CACHE);
		return "futslot/place";
	}

	@GetMapping({ "/futsal/{city}/{area}", "/ne/futsal/{city}/{area}" })
	String area(@PathVariable String city, @PathVariable String area, Model model, HttpServletRequest request,
			HttpServletResponse response) {
		model.addAttribute("dir", this.directory.area(city, area, wording(request)).orElseThrow(DirectoryController::notFound));
		response.setHeader("Cache-Control", CACHE);
		return "futslot/place";
	}

	@GetMapping({ "/venues/{slug}", "/ne/venues/{slug}" })
	String venue(@PathVariable String slug, @RequestParam(required = false) String date, Model model, HttpServletRequest request,
			HttpServletResponse response) {
		DirectoryService.VenuePage venue = this.directory.venue(slug, date, wording(request)).orElseThrow(DirectoryController::notFound);
		model.addAttribute("page", venue.page());
		model.addAttribute("dir", venue.chrome());
		model.addAttribute("app", venue.app());
		response.setHeader("Cache-Control", CACHE);
		return "futslot/venue";
	}

	@GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
	@ResponseBody
	String robots() {
		return this.directory.robots();
	}

	@GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
	@ResponseBody
	String sitemap() {
		return this.directory.sitemap();
	}

	private Wording wording(HttpServletRequest request) {
		boolean nepali = request.getRequestURI().equals(SiteLanguage.NEPALI) || request.getRequestURI().startsWith(SiteLanguage.NEPALI + "/");
		return this.language.of(nepali ? Languages.NEPALI : Languages.ENGLISH, request);
	}

	private static ResponseStatusException notFound() {
		return new ResponseStatusException(HttpStatus.NOT_FOUND);
	}

}
