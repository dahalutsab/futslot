package com.futslot.venue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.ApiException;

/** A venue's add-on catalogue (MANAGE_VENUE), and pricing add-ons for bookings. */
@Service
public class AddOnService {

	static final int MAX_QUANTITY = 50;

	private final AddOnItemRepository items;

	private final VenueRepository venues;

	private final VenueAccess access;

	AddOnService(AddOnItemRepository items, VenueRepository venues, VenueAccess access) {
		this.items = items;
		this.venues = venues;
		this.access = access;
	}

	@Transactional
	AddOnResponse create(UUID userId, UUID venueId, AddOnRequest request) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_VENUE);
		requireUniqueName(venueId, request.name(), new UUID(0, 0));
		return AddOnResponse.from(this.items.save(new AddOnItem(venueId, request)));
	}

	/** A new price applies to bookings from now on; existing bookings keep what they were charged. */
	@Transactional
	AddOnResponse update(UUID userId, UUID venueId, UUID itemId, AddOnRequest request) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_VENUE);
		AddOnItem item = this.items.findByIdAndVenueId(itemId, venueId).orElseThrow(AddOnService::notFound);
		requireUniqueName(venueId, request.name(), itemId);
		item.apply(request);
		return AddOnResponse.from(item);
	}

	@Transactional(readOnly = true)
	List<AddOnResponse> forVenue(UUID userId, UUID venueId) {
		this.access.require(userId, venueId, null);
		return this.items.findByVenueIdOrderByCategoryAscNameAsc(venueId).stream().map(AddOnResponse::from).toList();
	}

	@Transactional(readOnly = true)
	List<AddOnResponse> onSale(String slug) {
		Venue venue = this.venues.findBySlugAndStatus(slug, VenueStatus.ACTIVE).orElseThrow(VenueService::venueNotFound);
		return this.items.findByVenueIdAndActiveTrueOrderByCategoryAscNameAsc(venue.getId())
			.stream()
			.map(AddOnResponse::from)
			.toList();
	}

	/**
	 * The venue's active items for these quantities, keyed in request order; the same item listed twice is
	 * added up. For other modules pricing a booking.
	 */
	@Transactional(readOnly = true)
	public Map<AddOnItem, Integer> resolve(UUID venueId, List<AddOnLine> lines) {
		Map<UUID, Integer> quantities = new LinkedHashMap<>();
		if (lines != null) {
			lines.forEach(line -> quantities.merge(line.itemId(), line.quantity(), Integer::sum));
		}
		Map<AddOnItem, Integer> resolved = new LinkedHashMap<>();
		Map<UUID, AddOnItem> byId = new HashMap<>();
		this.items.findAllById(quantities.keySet()).forEach(item -> byId.put(item.getId(), item));
		quantities.forEach((itemId, quantity) -> {
			AddOnItem item = byId.get(itemId);
			if (item == null || !item.getVenueId().equals(venueId) || !item.isActive()) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "ADD_ON_NOT_AVAILABLE", "One of those extras isn't available here");
			}
			if (quantity > MAX_QUANTITY) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "TOO_MANY", "At most " + MAX_QUANTITY + " of each");
			}
			resolved.put(item, quantity);
		});
		return resolved;
	}

	private void requireUniqueName(UUID venueId, String name, UUID exceptId) {
		if (this.items.existsByVenueIdAndNameIgnoreCaseAndIdNot(venueId, name.strip(), exceptId)) {
			throw new ApiException(HttpStatus.CONFLICT, "ADD_ON_EXISTS", "You already have " + name.strip());
		}
	}

	private static ApiException notFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "ADD_ON_NOT_FOUND", "Add-on not found");
	}

}
