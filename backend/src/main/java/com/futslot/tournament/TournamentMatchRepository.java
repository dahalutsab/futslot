package com.futslot.tournament;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface TournamentMatchRepository extends JpaRepository<TournamentMatch, UUID> {

	@EntityGraph(attributePaths = { "home", "home.team", "away", "away.team", "court", "winner" })
	List<TournamentMatch> findByTournamentIdOrderByMatchNumber(UUID tournamentId);

	Optional<TournamentMatch> findByIdAndTournamentId(UUID id, UUID tournamentId);

	/** The two matches whose winners meet in {@code nextMatchId}, lower number (home side) first. */
	List<TournamentMatch> findByNextMatchIdOrderByMatchNumber(UUID nextMatchId);

	boolean existsByTournamentId(UUID tournamentId);

}
