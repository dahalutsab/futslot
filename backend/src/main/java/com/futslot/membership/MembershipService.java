package com.futslot.membership;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.booking.Booking;
import com.futslot.booking.BookingEvent;
import com.futslot.booking.BookingEventType;
import com.futslot.booking.BookingLookup;
import com.futslot.booking.VenueCustomer;
import com.futslot.common.ApiException;
import com.futslot.common.PageResponse;
import com.futslot.venue.Venue;
import com.futslot.venue.VenueAccess;
import com.futslot.venue.VenuePermission;
import com.futslot.venue.VenueRepository;
import com.futslot.venue.VenueStatus;

/**
 * Membership plans and the memberships sold from them. Money for a membership goes through payments
 * like a booking's; this service opens, activates and uses memberships.
 */
@Service
public class MembershipService {

	private final MembershipPlanRepository plans;

	private final MembershipRepository memberships;

	private final VenueRepository venues;

	private final VenueAccess access;

	private final BookingLookup bookings;

	private final Clock clock;

	MembershipService(MembershipPlanRepository plans, MembershipRepository memberships, VenueRepository venues,
			VenueAccess access, BookingLookup bookings, Clock clock) {
		this.plans = plans;
		this.memberships = memberships;
		this.venues = venues;
		this.access = access;
		this.bookings = bookings;
		this.clock = clock;
	}

	// Plans

	@Transactional
	MembershipPlanResponse createPlan(UUID userId, UUID venueId, MembershipPlanRequest request) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_VENUE);
		validate(request);
		return MembershipPlanResponse.from(this.plans.save(new MembershipPlan(venueId, request)));
	}

	/** Name, description, price and availability can always change; the benefits only until one is sold. */
	@Transactional
	MembershipPlanResponse updatePlan(UUID userId, UUID venueId, UUID planId, MembershipPlanRequest request) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_VENUE);
		validate(request);
		MembershipPlan plan = this.plans.findByIdAndVenueId(planId, venueId).orElseThrow(MembershipService::planNotFound);
		if (plan.changesBenefits(request) && this.memberships.existsByPlanId(planId)) {
			throw new ApiException(HttpStatus.CONFLICT, "PLAN_IN_USE",
					"Members already hold this plan, so its games, discount and validity can't change. Create a new plan instead");
		}
		plan.apply(request);
		return MembershipPlanResponse.from(plan);
	}

	@Transactional(readOnly = true)
	List<MembershipPlanResponse> plansForVenue(UUID userId, UUID venueId) {
		this.access.require(userId, venueId, null);
		return this.plans.findByVenueIdOrderByCreatedAt(venueId).stream().map(MembershipPlanResponse::from).toList();
	}

	@Transactional(readOnly = true)
	List<MembershipPlanResponse> plansOnSale(String slug) {
		Venue venue = this.venues.findBySlugAndStatus(slug, VenueStatus.ACTIVE)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VENUE_NOT_FOUND", "Venue not found"));
		return this.plans.findByVenueIdAndActiveTrueOrderByPrice(venue.getId())
			.stream()
			.map(MembershipPlanResponse::from)
			.toList();
	}

	// Selling (called by payments)

	/** An active plan on sale at an approved venue. */
	@Transactional(readOnly = true)
	public MembershipPlan planOnSale(UUID planId) {
		MembershipPlan plan = this.plans.findById(planId).filter(MembershipPlan::isActive)
			.orElseThrow(MembershipService::planNotFound);
		this.venues.findById(plan.getVenueId())
			.filter(venue -> venue.getStatus() == VenueStatus.ACTIVE)
			.orElseThrow(MembershipService::planNotFound);
		return plan;
	}

	/** An active plan of this venue, for staff selling at the counter. */
	@Transactional(readOnly = true)
	public MembershipPlan planAtVenue(UUID venueId, UUID planId) {
		return this.plans.findByIdAndVenueId(planId, venueId).filter(MembershipPlan::isActive)
			.orElseThrow(MembershipService::planNotFound);
	}

	/** Opened unpaid; {@link Membership#activate} once the money is in. */
	@Transactional
	public Membership open(MembershipPlan plan, Venue venue, VenueCustomer customer) {
		return this.memberships.save(new Membership(plan, venue, customer, today(venue)));
	}

	@Transactional
	public void activate(Membership membership) {
		membership.activate(today(membership.getVenue()));
	}

	// Managing

	@Transactional(readOnly = true)
	PageResponse<MembershipResponse> forVenue(UUID userId, UUID venueId, MembershipStatus status, Pageable pageable) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_BOOKINGS);
		return PageResponse.of((status != null) ? this.memberships.findByVenueIdAndStatus(venueId, status, pageable)
				: this.memberships.findByVenueId(venueId, pageable), MembershipResponse::from);
	}

	/** Any refund is between the venue and the member. */
	@Transactional
	MembershipResponse cancel(UUID userId, UUID venueId, UUID membershipId) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_BOOKINGS);
		Membership membership = this.memberships.findByIdAndVenueId(membershipId, venueId)
			.orElseThrow(MembershipService::notFound);
		if (membership.getStatus() == MembershipStatus.CANCELLED) {
			throw new ApiException(HttpStatus.CONFLICT, "MEMBERSHIP_CANCELLED", "This membership is already cancelled");
		}
		membership.cancel();
		return MembershipResponse.from(membership);
	}

	@Transactional(readOnly = true)
	List<MembershipResponse> mine(UUID userId) {
		return this.memberships.findForUser(userId).stream().map(MembershipResponse::from).toList();
	}

	// Using at booking time

	/**
	 * The customer's membership, if it can pay for a game on {@code playDate}. With {@code lock} the row is
	 * locked so concurrent bookings can't spend the same last game.
	 */
	@Transactional
	public Membership usable(UUID membershipId, VenueCustomer customer, LocalDate playDate, boolean lock) {
		Membership membership = (lock ? this.memberships.lockById(membershipId)
				: this.memberships.findById(membershipId))
			.filter(candidate -> customer != null && candidate.getCustomer().getId().equals(customer.getId()))
			.orElseThrow(MembershipService::notFound);
		if (membership.getStatus() != MembershipStatus.ACTIVE) {
			throw new ApiException(HttpStatus.CONFLICT, "MEMBERSHIP_NOT_ACTIVE",
					"This membership is " + membership.statusOn(today(membership.getVenue())));
		}
		if (playDate.isBefore(membership.getStartsOn()) || playDate.isAfter(membership.getExpiresOn())) {
			throw new ApiException(HttpStatus.CONFLICT, "MEMBERSHIP_NOT_VALID_ON_DATE",
					"This membership is valid " + membership.getStartsOn() + " to " + membership.getExpiresOn());
		}
		if (membership.getGamesRemaining() != null && membership.getGamesRemaining() <= 0) {
			throw new ApiException(HttpStatus.CONFLICT, "MEMBERSHIP_USED_UP", "No games left on this membership");
		}
		return membership;
	}

	/** A game pack covers the whole price; a pass takes its percentage off. */
	public static BigDecimal discountFor(Membership membership, BigDecimal price) {
		MembershipPlan plan = membership.getPlan();
		if (plan.getType() == MembershipType.GAME_PACK) {
			return price;
		}
		return price.multiply(BigDecimal.valueOf(plan.getDiscountPercent()))
			.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
	}

	/** Call on a membership from {@link #usable} with {@code lock}. */
	public void useGame(Membership membership) {
		membership.useGame();
	}

	/**
	 * The game comes back when the booking ends without being played, unless the player cancelled
	 * inside the venue's free-cancellation window (the same rule that decides a full refund).
	 */
	@EventListener
	void onBookingEnded(BookingEvent event) {
		if (event.type() != BookingEventType.CANCELLED && event.type() != BookingEventType.EXPIRED) {
			return;
		}
		Booking booking = this.bookings.byId(event.bookingId());
		if (booking.getMembershipId() == null) {
			return;
		}
		boolean byPlayer = event.type() == BookingEventType.CANCELLED && event.actorUserId() != null
				&& event.actorUserId().equals(event.playerUserId());
		Duration notice = Duration.between(this.clock.instant(), booking.getStartsAt());
		boolean late = notice.compareTo(Duration.ofHours(booking.getVenue().policy().freeCancelHours())) < 0;
		if (byPlayer && late) {
			return;
		}
		this.memberships.lockById(booking.getMembershipId()).ifPresent(Membership::returnGame);
	}

	private LocalDate today(Venue venue) {
		return LocalDate.now(this.clock.withZone(ZoneId.of(venue.getTimezone())));
	}

	private static void validate(MembershipPlanRequest request) {
		switch (request.type()) {
			case GAME_PACK -> {
				if (request.gamesIncluded() == null || request.discountPercent() != null) {
					throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PLAN",
							"A game pack needs gamesIncluded and no discountPercent");
				}
			}
			case MONTHLY_PASS -> {
				if (request.discountPercent() == null) {
					throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PLAN", "A pass needs a discountPercent");
				}
			}
		}
	}

	private static ApiException planNotFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "Membership plan not found");
	}

	private static ApiException notFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "MEMBERSHIP_NOT_FOUND", "Membership not found");
	}

}
