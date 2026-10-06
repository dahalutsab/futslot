package com.futslot.venue;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.PageResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** What players browse: only approved (ACTIVE) venues are visible. */
@RestController
@RequestMapping("/api/v1")
@SecurityRequirements
@Validated
class PublicVenueController {

	private final VenueService venueService;

	PublicVenueController(VenueService venueService) {
		this.venueService = venueService;
	}

	@GetMapping("/venues")
	PageResponse<VenueSummaryResponse> search(@RequestParam(required = false) String city,
			@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
		PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("averageRating"), Sort.Order.asc("name")));
		return this.venueService.searchActive(city, q, pageable);
	}

	@GetMapping("/venues/{slug}")
	VenueResponse get(@PathVariable String slug) {
		return this.venueService.getActiveBySlug(slug);
	}

	@GetMapping("/amenities")
	List<AmenityResponse> amenities() {
		return this.venueService.listAmenities();
	}

}
