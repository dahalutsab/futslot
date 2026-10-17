package com.futslot.deposit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface WithdrawalRepository extends JpaRepository<Withdrawal, UUID> {

	Optional<Withdrawal> findByUserIdAndStatus(UUID userId, WithdrawalStatus status);

	Optional<Withdrawal> findByIdAndUserId(UUID id, UUID userId);

	List<Withdrawal> findByStatusOrderByDueAt(WithdrawalStatus status);

}
