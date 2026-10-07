package com.futslot.membership;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/** {@code status} already accounts for the expiry date. */
public record MembershipResponse(UUID id, VenueRef venue, PlanRef plan, CustomerRef customer, LocalDate startsOn,
		LocalDate expiresOn, Integer gamesRemaining, MembershipStatus status, Instant createdAt) {

	public static MembershipResponse from(Membership membership) {
		MembershipPlan plan = membership.getPlan();
		LocalDate today = LocalDate.now(ZoneId.of(membership.getVenue().getTimezone()));
		return new MembershipResponse(membership.getId(),
				new VenueRef(membership.getVenue().getId(), membership.getVenue().getName(),
						membership.getVenue().getSlug()),
				new PlanRef(plan.getId(), plan.getName(), plan.getType(), plan.getGamesIncluded(),
						plan.getDiscountPercent()),
				new CustomerRef(membership.getCustomer().getId(), membership.getCustomer().getName(),
						membership.getCustomer().getPhone()),
				membership.getStartsOn(), membership.getExpiresOn(), membership.getGamesRemaining(),
				membership.statusOn(today), membership.getCreatedAt());
	}

	public record VenueRef(UUID id, String name, String slug) {

	}

	public record PlanRef(UUID id, String name, MembershipType type, Integer gamesIncluded, Integer discountPercent) {

	}

	public record CustomerRef(UUID id, String name, String phone) {

	}

}
