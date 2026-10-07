package com.futslot.tournament;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SquadMemberRepository extends JpaRepository<SquadMember, SquadMember.Key> {

	@EntityGraph(attributePaths = "user")
	List<SquadMember> findByRegistrationIdOrderByJerseyNumber(UUID registrationId);

	void deleteByRegistrationId(UUID registrationId);

	/** Players already named by another team that's still in the tournament. */
	@Query("""
			select s.userId from SquadMember s, Registration r
			where s.registrationId = r.id and r.tournament.id = :tournamentId and r.id <> :exceptRegistrationId
			  and r.status in (com.futslot.tournament.RegistrationStatus.PENDING_PAYMENT,
			                   com.futslot.tournament.RegistrationStatus.CONFIRMED,
			                   com.futslot.tournament.RegistrationStatus.WAITLISTED)
			  and s.userId in :userIds""")
	List<UUID> alreadyInOtherSquads(UUID tournamentId, UUID exceptRegistrationId, List<UUID> userIds);

	@Query("select s.userId from SquadMember s where s.registrationId in :registrationIds")
	List<UUID> playersOf(List<UUID> registrationIds);

}
