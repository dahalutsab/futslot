package com.futslot.notification;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface NotificationRepository extends JpaRepository<Notification, UUID> {

	Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

	Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

	long countByUserIdAndReadAtIsNull(UUID userId);

	@Modifying
	@Query("update Notification n set n.readAt = :now where n.userId = :userId and n.readAt is null")
	int markAllRead(UUID userId, Instant now);

}
