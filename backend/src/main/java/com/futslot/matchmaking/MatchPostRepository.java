package com.futslot.matchmaking;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

interface MatchPostRepository extends JpaRepository<MatchPost, UUID>, JpaSpecificationExecutor<MatchPost> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from MatchPost p where p.id = :id")
	Optional<MatchPost> lockById(UUID id);

	@EntityGraph(attributePaths = { "createdBy", "team", "venue" })
	Page<MatchPost> findByCreatedByIdOrderByStartsAtDesc(UUID userId, Pageable pageable);

	@Query("""
			select count(p) from MatchPost p
			where p.createdBy.id = :userId and p.status = com.futslot.matchmaking.MatchPostStatus.OPEN
			  and p.startsAt > :now""")
	long countOpenBy(UUID userId, Instant now);

}
