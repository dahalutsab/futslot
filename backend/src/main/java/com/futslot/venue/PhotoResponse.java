package com.futslot.venue;

import java.util.UUID;

/** {@code url} is a path on the API ({@code /media/...}); prefix it with the API's address. */
public record PhotoResponse(UUID id, String url, String caption, int sortOrder, boolean cover) {

	static PhotoResponse from(VenuePhoto photo) {
		return new PhotoResponse(photo.getId(), photo.getUrl(), photo.getCaption(), photo.getSortOrder(), photo.isCover());
	}

}
