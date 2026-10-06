package com.futslot.venue;

import java.time.Clock;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.ApiException;
import com.futslot.common.PageResponse;

/** Platform moderation: venues only appear publicly once an admin approves them. */
@Service
class AdminVenueService {

	private final VenueRepository venues;

	private final VenueMapper mapper;

	private final Clock clock;

	AdminVenueService(VenueRepository venues, VenueMapper mapper, Clock clock) {
		this.venues = venues;
		this.mapper = mapper;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	PageResponse<VenueSummaryResponse> list(VenueStatus status, Pageable pageable) {
		return PageResponse.of(this.venues.findByStatus(status, pageable), this.mapper::toSummary);
	}

	@Transactional(readOnly = true)
	VenueResponse get(UUID venueId) {
		return this.mapper.toResponse(venue(venueId), true);
	}

	@Transactional
	VenueResponse approve(UUID adminId, UUID venueId) {
		Venue venue = venueIn(venueId, VenueStatus.PENDING_APPROVAL);
		venue.approve(adminId, this.clock.instant());
		return this.mapper.toResponse(venue, true);
	}

	@Transactional
	VenueResponse reject(UUID venueId, String reason) {
		Venue venue = venueIn(venueId, VenueStatus.PENDING_APPROVAL);
		venue.reject(reason.strip());
		return this.mapper.toResponse(venue, true);
	}

	@Transactional
	VenueResponse suspend(UUID venueId, String reason) {
		Venue venue = venueIn(venueId, VenueStatus.ACTIVE);
		venue.suspend(reason.strip());
		return this.mapper.toResponse(venue, true);
	}

	@Transactional
	VenueResponse reinstate(UUID adminId, UUID venueId) {
		Venue venue = venueIn(venueId, VenueStatus.SUSPENDED);
		venue.approve(adminId, this.clock.instant());
		return this.mapper.toResponse(venue, true);
	}

	private Venue venue(UUID venueId) {
		return this.venues.findById(venueId).orElseThrow(VenueService::venueNotFound);
	}

	private Venue venueIn(UUID venueId, VenueStatus expected) {
		Venue venue = venue(venueId);
		if (venue.getStatus() != expected) {
			throw new ApiException(HttpStatus.CONFLICT, "INVALID_VENUE_STATUS",
					"Venue is " + venue.getStatus() + ", expected " + expected);
		}
		return venue;
	}

}
