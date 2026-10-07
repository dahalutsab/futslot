package com.futslot.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ConversationRepository extends JpaRepository<Conversation, UUID> {

	Optional<Conversation> findByVenueIdAndPlayerId(UUID venueId, UUID playerId);

	Optional<Conversation> findByIdAndPlayerId(UUID id, UUID playerId);

	Optional<Conversation> findByIdAndVenueId(UUID id, UUID venueId);

	@EntityGraph(attributePaths = { "venue", "player" })
	@Query("""
			select c from Conversation c where c.player.id = :playerId and c.lastMessageAt is not null
			order by c.lastMessageAt desc""")
	List<Conversation> findForPlayer(UUID playerId);

	@EntityGraph(attributePaths = { "venue", "player" })
	@Query("""
			select c from Conversation c where c.venue.id = :venueId and c.lastMessageAt is not null
			order by c.lastMessageAt desc""")
	List<Conversation> findForVenue(UUID venueId);

}
