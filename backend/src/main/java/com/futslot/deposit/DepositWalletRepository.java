package com.futslot.deposit;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

interface DepositWalletRepository extends JpaRepository<DepositWallet, UUID> {

	/** Locked until commit, so balance checks and changes for one player happen one at a time. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select w from DepositWallet w where w.userId = :userId")
	Optional<DepositWallet> lock(UUID userId);

	/** An empty wallet, unless the player has one already. */
	@Modifying
	@Query(value = "INSERT INTO deposit_wallets (user_id) VALUES (:userId) ON CONFLICT DO NOTHING", nativeQuery = true)
	void createIfMissing(UUID userId);

}
