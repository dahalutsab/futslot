package com.futslot.deposit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A player's booking deposit held by FutSlot. Only ever changed through {@link DepositEntry} rows, with the
 * wallet row locked, so the balance always equals the sum of its entries.
 */
@Entity
@Table(name = "deposit_wallets")
class DepositWallet {

	@Id
	private UUID userId;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal balance;

	@Column(nullable = false, updatable = false, insertable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected DepositWallet() {
	}

	void add(BigDecimal amount) {
		this.balance = this.balance.add(amount);
	}

	UUID getUserId() {
		return this.userId;
	}

	BigDecimal getBalance() {
		return this.balance;
	}

}
