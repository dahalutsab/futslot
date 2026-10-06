package com.futslot.notification;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

	Optional<DeviceToken> findByToken(String token);

	List<DeviceToken> findByUserId(UUID userId);

	@Modifying
	@Query("delete from DeviceToken d where d.token in :tokens")
	int deleteByTokenIn(Collection<String> tokens);

	@Modifying
	@Query("delete from DeviceToken d where d.token = :token and d.userId = :userId")
	int deleteByTokenAndUserId(String token, UUID userId);

}
