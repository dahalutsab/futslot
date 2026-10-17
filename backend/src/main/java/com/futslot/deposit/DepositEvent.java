package com.futslot.deposit;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published when deposit money moves in a way someone should hear about. {@code userId} is who to tell: the
 * player, or for PAYOUT_SENT the venue's owner. {@code balance} is the player's deposit afterwards (null for
 * PAYOUT_SENT). {@code detail} is what the money was for, worded to follow "for".
 */
public record DepositEvent(Type type, UUID userId, BigDecimal amount, BigDecimal balance, String venueName,
		String detail) {

	public enum Type {

		FEE_TAKEN, TOPPED_UP, WITHDRAWAL_PAID, PAYOUT_SENT

	}

}
