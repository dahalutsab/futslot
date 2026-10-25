package com.futslot.site;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * A demo (search indexing off: its venues are made up, or real places with made-up prices and free hours) says so
 * on every public page and venue website, so nobody takes it for the venue's own site or a real booking.
 */
@ControllerAdvice(assignableTypes = { DirectoryController.class, SitePageController.class, SiteBookingController.class })
class DemoNotice {

	private final SiteProperties properties;

	DemoNotice(SiteProperties properties) {
		this.properties = properties;
	}

	@ModelAttribute("demo")
	boolean demo() {
		return !this.properties.searchIndexing();
	}

}
