package com.futslot.team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

interface TeamRepository extends JpaRepository<Team, UUID>, JpaSpecificationExecutor<Team> {

	boolean existsBySlug(String slug);

	Optional<Team> findBySlug(String slug);

	@Query("""
			select t from Team t, TeamMember m
			where m.teamId = t.id and m.userId = :userId and m.status in :statuses
			order by t.name""")
	List<Team> findForUser(UUID userId, List<MemberStatus> statuses);

}
