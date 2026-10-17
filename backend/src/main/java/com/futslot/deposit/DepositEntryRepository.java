package com.futslot.deposit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface DepositEntryRepository extends JpaRepository<DepositEntry, UUID> {

	List<DepositEntry> findByUserIdOrderByCreatedAtDesc(UUID userId, Limit limit);

	/** Fees not yet in a payout, charged before {@code before}. */
	@Query("""
			select e from DepositEntry e where e.type = com.futslot.deposit.DepositEntryType.FEE
			  and e.payoutId is null and e.createdAt < :before order by e.createdAt""")
	List<DepositEntry> findUnpaidFeesBefore(Instant before);

	@Query("""
			select e from DepositEntry e left join fetch e.booking where e.type = com.futslot.deposit.DepositEntryType.FEE
			  and e.venueId = :venueId and e.payoutId is null order by e.createdAt desc""")
	List<DepositEntry> findUnpaidFees(UUID venueId);

}
