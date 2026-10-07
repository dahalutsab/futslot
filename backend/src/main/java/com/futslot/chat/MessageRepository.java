package com.futslot.chat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface MessageRepository extends JpaRepository<Message, UUID> {

	/** Newest first, older than {@code before} (exclusive). */
	@EntityGraph(attributePaths = "sender")
	@Query("""
			select m from Message m where m.conversation.id = :conversationId and m.createdAt < :before
			order by m.createdAt desc, m.id desc""")
	List<Message> findPage(UUID conversationId, Instant before, Pageable pageable);

	Optional<Message> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);

	/** Unread messages from the player ({@code fromPlayer}) or from the venue (everyone else). */
	@Query("""
			select count(m) from Message m
			where m.conversation.id = :conversationId and m.readAt is null
			  and ((:fromPlayer = true and m.sender.id = :playerId) or (:fromPlayer = false and m.sender.id <> :playerId))""")
	long countUnread(UUID conversationId, UUID playerId, boolean fromPlayer);

	@Modifying(flushAutomatically = true)
	@Query("""
			update Message m set m.readAt = :at
			where m.conversation.id = :conversationId and m.readAt is null
			  and ((:fromPlayer = true and m.sender.id = :playerId) or (:fromPlayer = false and m.sender.id <> :playerId))""")
	int markRead(UUID conversationId, UUID playerId, boolean fromPlayer, Instant at);

}
