package com.futslot.venue;

public record AmenityResponse(String code, String name, String icon) {

	static AmenityResponse from(Amenity amenity) {
		return new AmenityResponse(amenity.getCode(), amenity.getName(), amenity.getIcon());
	}

}
