package com.futslot.tournament;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface RegistrationRepository extends JpaRepository<Registration, UUID> {

	Optional<Registration> findByTournamentIdAndTeamId(UUID tournamentId, UUID teamId);

	Optional<Registration> findByIdAndTournamentId(UUID id, UUID tournamentId);

	@EntityGraph(attributePaths = "team")
	List<Registration> findByTournamentIdOrderByCreatedAt(UUID tournamentId);

	long countByTournamentIdAndStatusIn(UUID tournamentId, Collection<RegistrationStatus> statuses);

	@Query("""
			select r from Registration r
			where r.tournament.id = :tournamentId and r.status = com.futslot.tournament.RegistrationStatus.WAITLISTED
			order by r.createdAt""")
	List<Registration> waitlist(UUID tournamentId);

	@EntityGraph(attributePaths = { "team", "tournament", "tournament.venue" })
	@Query("""
			select r from Registration r
			where r.team.id in (select m.teamId from TeamMember m
			                    where m.userId = :userId and m.status = com.futslot.team.MemberStatus.ACTIVE)
			order by r.createdAt desc""")
	List<Registration> findForPlayer(UUID userId);

}
