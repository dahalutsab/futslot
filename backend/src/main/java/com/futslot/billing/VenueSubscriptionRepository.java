package com.futslot.billing;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

interface VenueSubscriptionRepository extends JpaRepository<VenueSubscription, UUID> {

	/** The venue's live subscription (at most one, enforced by the database). */
	@EntityGraph(attributePaths = { "plan", "nextPlan" })
	@Query("""
			select s from VenueSubscription s where s.venueId = :venueId
			and s.status in (com.futslot.billing.SubscriptionStatus.TRIALING, com.futslot.billing.SubscriptionStatus.ACTIVE,
			                 com.futslot.billing.SubscriptionStatus.PAST_DUE)""")
	Optional<VenueSubscription> findLive(UUID venueId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from VenueSubscription s where s.id = :id")
	Optional<VenueSubscription> lockById(UUID id);

	@EntityGraph(attributePaths = { "plan", "nextPlan" })
	List<VenueSubscription> findByStatusIn(Collection<SubscriptionStatus> statuses);

}
