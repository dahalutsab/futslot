package com.futslot.booking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface RecurringBookingRepository extends JpaRepository<RecurringBooking, UUID> {

	Optional<RecurringBooking> findByIdAndVenueId(UUID id, UUID venueId);

	List<RecurringBooking> findByVenueIdAndStatusNotOrderByDayOfWeekAscStartTimeAsc(UUID venueId, RecurringStatus status);

	/** Active series whose generated occurrences don't yet reach {@code horizon}. */
	@Query("""
			select r.id from RecurringBooking r
			where r.status = com.futslot.booking.RecurringStatus.ACTIVE
			  and (r.generatedUntil is null or r.generatedUntil < :horizon)
			  and (r.endsOn is null or r.generatedUntil is null or r.generatedUntil < r.endsOn)""")
	List<UUID> findNeedingGeneration(LocalDate horizon);

}
