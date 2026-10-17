package com.futslot.deposit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.futslot.booking.Booking;
import com.futslot.common.Timestamps;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** One change to a player's deposit: positive money in, negative money out. */
@Entity
@Table(name = "deposit_entries")
class DepositEntry {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false, updatable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private DepositEntryType type;

	@Column(nullable = false, precision = 12, scale = 2, updatable = false)
	private BigDecimal amount;

	@Column(nullable = false, precision = 12, scale = 2, updatable = false)
	private BigDecimal balanceAfter;

	@Column(updatable = false)
	private UUID paymentId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "booking_id", updatable = false)
	private Booking booking;

	@Column(updatable = false)
	private UUID venueId;

	@Column(updatable = false)
	private UUID withdrawalId;

	/** Set once the fee is part of a monthly payout to the venue. */
	private UUID payoutId;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected DepositEntry() {
	}

	private DepositEntry(DepositWallet wallet, DepositEntryType type, BigDecimal amount) {
		wallet.add(amount);
		this.userId = wallet.getUserId();
		this.type = type;
		this.amount = amount;
		this.balanceAfter = wallet.getBalance();
	}

	static DepositEntry topUp(DepositWallet wallet, BigDecimal amount, UUID paymentId) {
		DepositEntry entry = new DepositEntry(wallet, DepositEntryType.TOP_UP, amount);
		entry.paymentId = paymentId;
		return entry;
	}

	static DepositEntry fee(DepositWallet wallet, BigDecimal amount, Booking booking) {
		DepositEntry entry = new DepositEntry(wallet, DepositEntryType.FEE, amount.negate());
		entry.booking = booking;
		entry.venueId = booking.getVenue().getId();
		return entry;
	}

	static DepositEntry withdrawal(DepositWallet wallet, Withdrawal withdrawal) {
		DepositEntry entry = new DepositEntry(wallet, DepositEntryType.WITHDRAWAL, withdrawal.getAmount().negate());
		entry.withdrawalId = withdrawal.getId();
		return entry;
	}

	static DepositEntry withdrawalCancelled(DepositWallet wallet, Withdrawal withdrawal) {
		DepositEntry entry = new DepositEntry(wallet, DepositEntryType.WITHDRAWAL_CANCELLED, withdrawal.getAmount());
		entry.withdrawalId = withdrawal.getId();
		return entry;
	}

	void paidOutIn(Payout payout) {
		this.payoutId = payout.getId();
	}

	UUID getId() {
		return this.id;
	}

	DepositEntryType getType() {
		return this.type;
	}

	BigDecimal getAmount() {
		return this.amount;
	}

	BigDecimal getBalanceAfter() {
		return this.balanceAfter;
	}

	Booking getBooking() {
		return this.booking;
	}

	UUID getVenueId() {
		return this.venueId;
	}

	Instant getCreatedAt() {
		return this.createdAt;
	}

	@PrePersist
	void stampCreated() {
		if (this.createdAt == null) {
			this.createdAt = Timestamps.now();
		}
	}

}
