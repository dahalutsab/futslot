package com.futslot.venue;

import java.time.Clock;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
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

	private final ApplicationEventPublisher events;

	private final Clock clock;

	AdminVenueService(VenueRepository venues, VenueMapper mapper, ApplicationEventPublisher events, Clock clock) {
		this.venues = venues;
		this.mapper = mapper;
		this.events = events;
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
		return moderated(venue, adminId);
	}

	@Transactional
	VenueResponse reject(UUID adminId, UUID venueId, String reason) {
		Venue venue = venueIn(venueId, VenueStatus.PENDING_APPROVAL);
		venue.reject(reason.strip());
		return moderated(venue, adminId);
	}

	@Transactional
	VenueResponse suspend(UUID adminId, UUID venueId, String reason) {
		Venue venue = venueIn(venueId, VenueStatus.ACTIVE);
		venue.suspend(reason.strip());
		return moderated(venue, adminId);
	}

	@Transactional
	VenueResponse reinstate(UUID adminId, UUID venueId) {
		Venue venue = venueIn(venueId, VenueStatus.SUSPENDED);
		venue.approve(adminId, this.clock.instant());
		return moderated(venue, adminId);
	}

	private VenueResponse moderated(Venue venue, UUID adminId) {
		this.events.publishEvent(VenueModerated.of(venue, adminId));
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
