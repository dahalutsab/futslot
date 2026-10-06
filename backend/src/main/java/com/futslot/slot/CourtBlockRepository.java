package com.futslot.slot;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CourtBlockRepository extends JpaRepository<CourtBlock, UUID> {

	@Query("""
			select b from CourtBlock b
			where b.courtId in :courtIds and b.startsAt < :to and b.endsAt > :from
			order by b.startsAt""")
	List<CourtBlock> findOverlapping(Collection<UUID> courtIds, Instant from, Instant to);

	Optional<CourtBlock> findByIdAndCourtId(UUID id, UUID courtId);

}
