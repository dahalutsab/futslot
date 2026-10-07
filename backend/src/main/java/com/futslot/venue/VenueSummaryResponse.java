package com.futslot.venue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VenueSummaryResponse(UUID id, String slug, String name, String area, String city, BigDecimal latitude,
		BigDecimal longitude, VenueStatus status, BigDecimal averageRating, int reviewCount, BigDecimal fromPrice,
		int courtCount, List<String> amenities, String coverPhotoUrl) {

}
