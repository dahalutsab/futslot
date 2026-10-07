package com.futslot.venue;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A player's saved venues. Only approved venues can be saved, and only those are listed. */
@Service
class FavouriteService {

	private final JdbcClient jdbc;

	private final VenueRepository venues;

	private final VenueMapper mapper;

	FavouriteService(JdbcClient jdbc, VenueRepository venues, VenueMapper mapper) {
		this.jdbc = jdbc;
		this.venues = venues;
		this.mapper = mapper;
	}

	/** Most recently saved first. */
	@Transactional(readOnly = true)
	List<VenueSummaryResponse> list(UUID userId) {
		List<UUID> ids = this.jdbc
			.sql("SELECT venue_id FROM favourite_venues WHERE user_id = :userId ORDER BY created_at DESC")
			.param("userId", userId)
			.query(UUID.class)
			.list();
		Map<UUID, Venue> byId = this.venues.findAllById(ids)
			.stream()
			.filter(venue -> venue.getStatus() == VenueStatus.ACTIVE)
			.collect(Collectors.toMap(Venue::getId, Function.identity()));
		return ids.stream().filter(byId::containsKey).map(byId::get).map(this.mapper::toSummary).toList();
	}

	/** Saving twice is fine. */
	@Transactional
	void add(UUID userId, UUID venueId) {
		this.venues.findById(venueId)
			.filter(venue -> venue.getStatus() == VenueStatus.ACTIVE)
			.orElseThrow(VenueService::venueNotFound);
		this.jdbc.sql("INSERT INTO favourite_venues (user_id, venue_id) VALUES (:userId, :venueId) ON CONFLICT DO NOTHING")
			.param("userId", userId)
			.param("venueId", venueId)
			.update();
	}

	@Transactional
	void remove(UUID userId, UUID venueId) {
		this.jdbc.sql("DELETE FROM favourite_venues WHERE user_id = :userId AND venue_id = :venueId")
			.param("userId", userId)
			.param("venueId", venueId)
			.update();
	}

}
