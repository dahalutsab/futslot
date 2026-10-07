package com.futslot.matchmaking;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface JoinRequestRepository extends JpaRepository<JoinRequest, UUID> {

	Optional<JoinRequest> findByPostIdAndUserId(UUID postId, UUID userId);

	Optional<JoinRequest> findByIdAndPostId(UUID id, UUID postId);

	@EntityGraph(attributePaths = { "user", "team" })
	List<JoinRequest> findByPostIdOrderByCreatedAt(UUID postId);

	List<JoinRequest> findByPostIdAndStatus(UUID postId, JoinRequestStatus status);

	long countByPostIdAndStatus(UUID postId, JoinRequestStatus status);

	@EntityGraph(attributePaths = { "post", "post.createdBy", "post.team", "post.venue", "team" })
	Page<JoinRequest> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

}
