package com.futslot.team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMember.Key> {

	Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

	@EntityGraph(attributePaths = "user")
	List<TeamMember> findByTeamIdAndStatusInOrderByCreatedAt(UUID teamId, List<MemberStatus> statuses);

	long countByTeamIdAndStatusIn(UUID teamId, List<MemberStatus> statuses);

}
