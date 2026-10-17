package com.futslot.deposit;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface PayoutRepository extends JpaRepository<Payout, UUID> {

	Optional<Payout> findByVenueIdAndPeriodStartAndPeriodEnd(UUID venueId, LocalDate start, LocalDate end);

	List<Payout> findByStatusOrderByPeriodStartAscVenueId(PayoutStatus status);

	List<Payout> findTop12ByVenueIdOrderByPeriodStartDesc(UUID venueId);

}
