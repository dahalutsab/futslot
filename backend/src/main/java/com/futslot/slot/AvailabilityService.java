package com.futslot.slot;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.ApiException;
import com.futslot.venue.Court;
import com.futslot.venue.OperatingHours;
import com.futslot.venue.PricingRule;
import com.futslot.venue.Venue;
import com.futslot.venue.VenueRepository;
import com.futslot.venue.VenueStatus;

@Service
public class AvailabilityService {

	private final VenueRepository venues;

	private final BookedRanges bookedRanges;

	private final CourtBlockRepository blocks;

	private final Clock clock;

	AvailabilityService(VenueRepository venues, BookedRanges bookedRanges, CourtBlockRepository blocks, Clock clock) {
		this.venues = venues;
		this.bookedRanges = bookedRanges;
		this.blocks = blocks;
		this.clock = clock;
	}

	/** What players see: only within the venue's booking window (today up to max advance days). */
	@Transactional(readOnly = true)
	public AvailabilityResponse forPlayers(String slug, LocalDate date) {
		Venue venue = this.venues.findBySlugAndStatus(slug, VenueStatus.ACTIVE)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VENUE_NOT_FOUND", "Venue not found"));
		LocalDate today = LocalDate.now(this.clock.withZone(ZoneId.of(venue.getTimezone())));
		LocalDate last = today.plusDays(venue.policy().maxAdvanceBookingDays());
		if (date.isBefore(today) || date.isAfter(last)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "DATE_OUT_OF_RANGE",
					"Pick a date between " + today + " and " + last);
		}
		return compute(venue, date);
	}

	/** The owner's calendar: any date, any venue status. */
	@Transactional(readOnly = true)
	public AvailabilityResponse forOwner(UUID ownerId, UUID venueId, LocalDate date) {
		Venue venue = this.venues.findByIdAndOwnerId(venueId, ownerId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VENUE_NOT_FOUND", "Venue not found"));
		return compute(venue, date);
	}

	/** Slots of one court for an operating day; used by bookings to validate and price a requested slot. */
	@Transactional(readOnly = true)
	public List<Slot> slotsFor(Venue venue, Court court, LocalDate date) {
		return compute(venue, date).courts()
			.stream()
			.filter(courtSlots -> courtSlots.courtId().equals(court.getId()))
			.findFirst()
			.map(AvailabilityResponse.CourtSlots::slots)
			.orElse(List.of());
	}

	private AvailabilityResponse compute(Venue venue, LocalDate date) {
		ZoneId zone = ZoneId.of(venue.getTimezone());
		Instant now = this.clock.instant();
		OperatingHours hours = venue.getOperatingHours()
			.stream()
			.filter(day -> day.dayOfWeek() == date.getDayOfWeek())
			.findFirst()
			.orElse(null);
		List<Court> courts = venue.getCourts().stream().filter(Court::isActive).toList();
		List<UUID> courtIds = courts.stream().map(Court::getId).toList();

		// Operating days can run past midnight, so look at the whole next day too
		Instant from = date.atStartOfDay(zone).toInstant();
		Instant to = date.plusDays(2).atStartOfDay(zone).toInstant();
		Map<UUID, List<TimeRange>> booked = this.bookedRanges.byCourt(courtIds, from, to, now);
		Map<UUID, List<TimeRange>> blocked = courtIds.isEmpty() ? Map.of()
				: this.blocks.findOverlapping(courtIds, from, to)
					.stream()
					.collect(Collectors.groupingBy(CourtBlock::getCourtId,
							Collectors.mapping(CourtBlock::range, Collectors.toList())));

		List<AvailabilityResponse.CourtSlots> courtSlots = courts.stream().map(court -> {
			List<PriceRule> rules = court.getPricingRules()
				.stream()
				.filter(PricingRule::isActive)
				.map(PriceRule::from)
				.toList();
			List<Slot> slots = SlotCalculator.slots(date, zone, hours, court.getSlotDurationMinutes(), rules,
					booked.getOrDefault(court.getId(), List.of()), blocked.getOrDefault(court.getId(), List.of()), now);
			return new AvailabilityResponse.CourtSlots(court.getId(), court.getName(), court.getFormat(),
					court.getSlotDurationMinutes(), slots);
		}).toList();
		return new AvailabilityResponse(venue.getId(), date, venue.getTimezone(), courtSlots);
	}

}
