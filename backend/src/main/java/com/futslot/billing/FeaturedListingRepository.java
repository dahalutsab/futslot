package com.futslot.billing;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface FeaturedListingRepository extends JpaRepository<FeaturedListing, UUID> {

	@EntityGraph(attributePaths = "venue")
	List<FeaturedListing> findByVenueIdOrderByStartsAtDesc(UUID venueId);

	@EntityGraph(attributePaths = "venue")
	List<FeaturedListing> findByStatusInOrderByStartsAt(Collection<FeaturedStatus> statuses);

	/**
	 * Listings taking a place in [start, end): paid ones, and unpaid ones still within their time to pay
	 * (so a place can't be sold twice while a payment is being checked).
	 */
	@Query("""
			select l from FeaturedListing l
			where l.placement = :placement and (:city is null or l.city = :city)
			  and l.startsAt < :end and l.endsAt > :start
			  and (l.status in (com.futslot.billing.FeaturedStatus.SCHEDULED, com.futslot.billing.FeaturedStatus.ACTIVE)
			       or (l.status = com.futslot.billing.FeaturedStatus.PENDING_PAYMENT and l.createdAt > :unpaidSince))""")
	List<FeaturedListing> findTaking(FeaturedPlacement placement, String city, Instant start, Instant end,
			Instant unpaidSince);

	@Query("""
			select l from FeaturedListing l
			where l.placement in :placements and l.startsAt <= :now and l.endsAt > :now
			  and l.status in (com.futslot.billing.FeaturedStatus.SCHEDULED, com.futslot.billing.FeaturedStatus.ACTIVE)
			order by l.startsAt""")
	List<FeaturedListing> findLive(Collection<FeaturedPlacement> placements, Instant now);

}
