package com.futslot.venue;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/** Builds API views of a venue. Must be called inside a transaction (courts and rules are lazy). */
@Component
class VenueMapper {

	private final AmenityRepository amenities;

	private final VenuePhotoRepository photos;

	VenueMapper(AmenityRepository amenities, VenuePhotoRepository photos) {
		this.amenities = amenities;
		this.photos = photos;
	}

	VenueResponse toResponse(Venue venue, boolean ownerView) {
		Map<String, Amenity> amenityByCode = this.amenities.findAll()
			.stream()
			.collect(Collectors.toMap(Amenity::getCode, Function.identity()));
		List<AmenityResponse> amenities = venue.getAmenityCodes()
			.stream()
			.sorted()
			.map(amenityByCode::get)
			.map(AmenityResponse::from)
			.toList();
		List<CourtResponse> courts = venue.getCourts()
			.stream()
			.filter(court -> ownerView || court.isActive())
			.map(court -> CourtResponse.from(court, ownerView))
			.toList();
		List<DayHours> hours = venue.getOperatingHours()
			.stream()
			.sorted(Comparator.comparing(OperatingHours::dayOfWeek))
			.map(DayHours::from)
			.toList();
		return new VenueResponse(venue.getId(), venue.getSlug(), venue.getName(), venue.getDescription(),
				venue.getPhone(), venue.getEmail(), venue.getAddressLine(), venue.getArea(), venue.getCity(),
				venue.getLatitude(), venue.getLongitude(), venue.getTimezone(), venue.getStatus(),
				ownerView ? venue.getRejectionReason() : null, venue.policy(), venue.getPaymentInstructions(),
				venue.getAverageRating(),
				venue.getReviewCount(), fromPrice(venue), hours, amenities, courts,
				this.photos.findByVenueIdOrderByIsCoverDescSortOrderAscCreatedAtAsc(venue.getId())
					.stream()
					.map(PhotoResponse::from)
					.toList());
	}

	VenueSummaryResponse toSummary(Venue venue) {
		int activeCourts = (int) venue.getCourts().stream().filter(Court::isActive).count();
		return new VenueSummaryResponse(venue.getId(), venue.getSlug(), venue.getName(), venue.getArea(),
				venue.getCity(), venue.getLatitude(), venue.getLongitude(), venue.getStatus(), venue.getAverageRating(),
				venue.getReviewCount(), fromPrice(venue), activeCourts, venue.getAmenityCodes().stream().sorted().toList(),
				this.photos.findByVenueIdAndIsCoverTrue(venue.getId()).map(VenuePhoto::getUrl).orElse(null));
	}

	/** Cheapest active price across active courts: the "from Rs X/hr" shown in listings. */
	private static BigDecimal fromPrice(Venue venue) {
		return venue.getCourts()
			.stream()
			.filter(Court::isActive)
			.flatMap(court -> court.getPricingRules().stream())
			.filter(PricingRule::isActive)
			.map(PricingRule::getPrice)
			.min(Comparator.naturalOrder())
			.orElse(null);
	}

}
