package com.futslot.tournament;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

interface TournamentRepository extends JpaRepository<Tournament, UUID>, JpaSpecificationExecutor<Tournament> {

	boolean existsBySlug(String slug);

	Optional<Tournament> findBySlug(String slug);

	Optional<Tournament> findByIdAndVenueId(UUID id, UUID venueId);

	List<Tournament> findByVenueIdOrderByStartsOnDesc(UUID venueId);

	/** Registrations and draws for one tournament run one at a time, so places can't be oversold. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from Tournament t where t.id = :id")
	Optional<Tournament> lockById(UUID id);

}
