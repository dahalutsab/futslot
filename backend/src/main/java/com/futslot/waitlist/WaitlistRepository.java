package com.futslot.waitlist;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface WaitlistRepository extends JpaRepository<WaitlistEntry, UUID> {

	Optional<WaitlistEntry> findByCourtIdAndUserIdAndStartsAt(UUID courtId, UUID userId, Instant startsAt);

	Optional<WaitlistEntry> findByIdAndUserId(UUID id, UUID userId);

	@EntityGraph(attributePaths = { "court", "court.venue" })
	List<WaitlistEntry> findByUserIdOrderByStartsAtDesc(UUID userId);

	@Query("""
			select count(e) from WaitlistEntry e
			where e.userId = :userId and e.startsAt > :now
			  and e.status in (com.futslot.waitlist.WaitlistStatus.WAITING, com.futslot.waitlist.WaitlistStatus.NOTIFIED)""")
	long countOpen(UUID userId, Instant now);

	/** Entries for slots overlapping [start, end) on the court that are still waiting, first come first. */
	@Query("""
			select e from WaitlistEntry e
			where e.court.id = :courtId and e.startsAt < :end and e.endsAt > :start and e.startsAt > :now
			  and e.status in (com.futslot.waitlist.WaitlistStatus.WAITING, com.futslot.waitlist.WaitlistStatus.NOTIFIED)
			order by e.createdAt""")
	List<WaitlistEntry> findOpenOverlapping(UUID courtId, Instant start, Instant end, Instant now);

}
