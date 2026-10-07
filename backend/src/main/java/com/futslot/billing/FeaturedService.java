package com.futslot.billing;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.futslot.common.ApiException;
import com.futslot.venue.Venue;
import com.futslot.venue.VenueAccess;
import com.futslot.venue.VenueService;
import com.futslot.venue.VenueStatus;
import com.futslot.venue.VenueSummaryResponse;

/**
 * Venues paying to be shown first, by the day. Each placement has a price per day and a number of places
 * (per city for CITY_TOP). An unpaid listing holds its place for {@link #TIME_TO_PAY}, then lapses.
 */
@Service
public class FeaturedService {

	static final Duration TIME_TO_PAY = Duration.ofHours(48);

	private static final ZoneId NEPAL = ZoneId.of("Asia/Kathmandu");

	private final FeaturedListingRepository listings;

	private final VenueAccess access;

	private final VenueService venues;

	private final BillingProperties properties;

	private final JdbcClient jdbc;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	FeaturedService(FeaturedListingRepository listings, VenueAccess access, VenueService venues,
			BillingProperties properties, JdbcClient jdbc, ApplicationEventPublisher events, Clock clock) {
		this.listings = listings;
		this.access = access;
		this.venues = venues;
		this.properties = properties;
		this.jdbc = jdbc;
		this.events = events;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	BillingViews.FeaturedQuote quote(UUID userId, UUID venueId, FeaturedPlacement placement, LocalDate startsOn, int days) {
		Venue venue = this.access.requireOwner(userId, venueId);
		BillingProperties.Placement rate = this.properties.placement(placement);
		return new BillingViews.FeaturedQuote(placement, startsOn, days, rate.pricePerDay(),
				rate.pricePerDay().multiply(BigDecimal.valueOf(days)), hasRoom(venue, placement, startsOn, days));
	}

	@Transactional
	BillingViews.Featured book(UUID userId, UUID venueId, BillingRequests.Feature request) {
		Venue venue = this.access.requireOwner(userId, venueId);
		if (venue.getStatus() != VenueStatus.ACTIVE) {
			throw SubscriptionService.conflict("VENUE_NOT_LIVE", "Only approved venues can be featured");
		}
		if (request.startsOn().isBefore(LocalDate.now(this.clock.withZone(NEPAL)))) {
			throw SubscriptionService.badRequest("START_IN_PAST", "Pick today or a later day");
		}
		// One booking of each placement at a time, so two venues can't both take the last place
		this.jdbc.sql("SELECT pg_advisory_xact_lock(hashtext(:key))")
			.param("key", "featured:" + request.placement())
			.query()
			.singleRow();
		Instant start = request.startsOn().atStartOfDay(NEPAL).toInstant();
		Instant end = request.startsOn().plusDays(request.days()).atStartOfDay(NEPAL).toInstant();
		List<FeaturedListing> taking = taking(venue, request.placement(), start, end);
		if (taking.stream().anyMatch(listing -> listing.getVenue().getId().equals(venueId))) {
			throw SubscriptionService.conflict("ALREADY_FEATURED", "You're already featured there for some of those days");
		}
		if (taking.size() >= this.properties.placement(request.placement()).maxLive()) {
			throw SubscriptionService.conflict("PLACEMENT_FULL", "All featured places are taken for those days");
		}
		BigDecimal price = this.properties.placement(request.placement()).pricePerDay()
			.multiply(BigDecimal.valueOf(request.days()));
		return view(this.listings.save(new FeaturedListing(venue, request.placement(), start, end, price)));
	}

	@Transactional
	BillingViews.Featured cancel(UUID userId, UUID venueId, UUID listingId) {
		this.access.requireOwner(userId, venueId);
		FeaturedListing listing = this.listings.findById(listingId)
			.filter(found -> found.getVenue().getId().equals(venueId))
			.orElseThrow(FeaturedService::notFound);
		if (listing.getStatus() != FeaturedStatus.PENDING_PAYMENT) {
			throw SubscriptionService.conflict("ALREADY_PAID", "A paid listing can't be cancelled here. Contact FutSlot");
		}
		listing.setStatus(FeaturedStatus.CANCELLED);
		return view(listing);
	}

	@Transactional(readOnly = true)
	List<BillingViews.Featured> forVenue(UUID userId, UUID venueId) {
		this.access.requireOwner(userId, venueId);
		return views(this.listings.findByVenueIdOrderByStartsAtDesc(venueId));
	}

	@Transactional(readOnly = true)
	List<BillingViews.Featured> byStatus(Set<FeaturedStatus> statuses) {
		return views(this.listings.findByStatusInOrderByStartsAt(statuses));
	}

	/**
	 * Venues featured right now. SEARCH_TOP also brings in CITY_TOP listings for {@code city}; with a city,
	 * only venues in it are returned.
	 */
	@Transactional(readOnly = true)
	List<VenueSummaryResponse> live(FeaturedPlacement placement, String city) {
		Set<FeaturedPlacement> placements = (placement == FeaturedPlacement.SEARCH_TOP && StringUtils.hasText(city))
				? EnumSet.of(FeaturedPlacement.SEARCH_TOP, FeaturedPlacement.CITY_TOP) : EnumSet.of(placement);
		List<UUID> venueIds = this.listings.findLive(placements, this.clock.instant())
			.stream()
			.filter(listing -> !StringUtils.hasText(city) || listing.getVenue().getCity().equalsIgnoreCase(city.strip()))
			.map(listing -> listing.getVenue().getId())
			.toList();
		return this.venues.summaries(venueIds);
	}

	// For payments

	/** An unpaid listing of a venue {@code userId} owns, still within its time to pay. */
	@Transactional(readOnly = true)
	public FeaturedListing listingToPay(UUID userId, UUID listingId) {
		FeaturedListing listing = this.listings.findById(listingId).orElseThrow(FeaturedService::notFound);
		this.access.requireOwner(userId, listing.getVenue().getId());
		return requireUnpaid(listing);
	}

	@Transactional(readOnly = true)
	public FeaturedListing unpaidListing(UUID listingId) {
		return requireUnpaid(this.listings.findById(listingId).orElseThrow(FeaturedService::notFound));
	}

	@Transactional
	public void listingPaid(FeaturedListing listing) {
		listing.paid(this.clock.instant());
		Venue venue = listing.getVenue();
		this.events.publishEvent(new BillingEvent(BillingEvent.Type.FEATURED_CONFIRMED, venue.getId(), venue.getName(),
				venue.getOwnerId(), null, listing.getPrice(), listing.getStartsAt().atZone(NEPAL).toLocalDate()));
	}

	/** Lapses unpaid listings and moves paid ones along by time. Runs hourly from {@link BillingJobs}. */
	@Transactional
	public void maintain() {
		Instant now = this.clock.instant();
		for (FeaturedListing listing : this.listings.findByStatusInOrderByStartsAt(EnumSet.of(
				FeaturedStatus.PENDING_PAYMENT, FeaturedStatus.SCHEDULED, FeaturedStatus.ACTIVE))) {
			if (listing.getStatus() == FeaturedStatus.PENDING_PAYMENT) {
				if (!now.isBefore(listing.getCreatedAt().plus(TIME_TO_PAY))) {
					listing.setStatus(FeaturedStatus.CANCELLED);
				}
			}
			else {
				listing.setStatus(listing.statusAt(now));
			}
		}
	}

	private boolean hasRoom(Venue venue, FeaturedPlacement placement, LocalDate startsOn, int days) {
		Instant start = startsOn.atStartOfDay(NEPAL).toInstant();
		Instant end = startsOn.plusDays(days).atStartOfDay(NEPAL).toInstant();
		List<FeaturedListing> taking = taking(venue, placement, start, end);
		return taking.stream().noneMatch(listing -> listing.getVenue().getId().equals(venue.getId()))
				&& taking.size() < this.properties.placement(placement).maxLive();
	}

	private List<FeaturedListing> taking(Venue venue, FeaturedPlacement placement, Instant start, Instant end) {
		String city = (placement == FeaturedPlacement.CITY_TOP) ? venue.getCity() : null;
		return this.listings.findTaking(placement, city, start, end, this.clock.instant().minus(TIME_TO_PAY));
	}

	private FeaturedListing requireUnpaid(FeaturedListing listing) {
		if (listing.getStatus() != FeaturedStatus.PENDING_PAYMENT
				|| !this.clock.instant().isBefore(listing.getCreatedAt().plus(TIME_TO_PAY))) {
			throw SubscriptionService.conflict("LISTING_NOT_PAYABLE", "This listing is no longer waiting for payment");
		}
		return listing;
	}

	private List<BillingViews.Featured> views(List<FeaturedListing> listings) {
		if (listings.isEmpty()) {
			return List.of();
		}
		Set<UUID> pending = new HashSet<>(this.listings.withPendingPayment(listings.stream().map(FeaturedListing::getId).toList()));
		return listings.stream().map(listing -> view(listing, pending.contains(listing.getId()))).toList();
	}

	private BillingViews.Featured view(FeaturedListing listing) {
		return views(List.of(listing)).getFirst();
	}

	private BillingViews.Featured view(FeaturedListing listing, boolean paymentPending) {
		return new BillingViews.Featured(listing.getId(), listing.getVenue().getId(), listing.getVenue().getName(),
				listing.getPlacement(), listing.getCity(), listing.getStartsAt(), listing.getEndsAt(), listing.getPrice(),
				listing.statusAt(this.clock.instant()), this.properties.paymentInstructions(), paymentPending);
	}

	private static ApiException notFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "LISTING_NOT_FOUND", "Featured listing not found");
	}

}
