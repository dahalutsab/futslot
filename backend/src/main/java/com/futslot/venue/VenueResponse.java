package com.futslot.venue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VenueResponse(UUID id, String slug, String name, String description, String phone, String email,
		String addressLine, String area, String city, BigDecimal latitude, BigDecimal longitude, String timezone,
		VenueStatus status, String rejectionReason, VenuePolicy policy, String paymentInstructions,
		BigDecimal averageRating, int reviewCount,
		BigDecimal fromPrice, List<DayHours> operatingHours, List<AmenityResponse> amenities,
		List<CourtResponse> courts) {

}
