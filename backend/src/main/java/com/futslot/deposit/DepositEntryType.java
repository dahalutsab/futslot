package com.futslot.deposit;

public enum DepositEntryType {

	/** Money the player sent FutSlot, once an admin found it. */
	TOP_UP,

	/** A late cancellation or no-show fee the advance didn't cover, owed to the venue. */
	FEE,

	/** The whole balance set aside to send back to the player. */
	WITHDRAWAL,

	/** The player called off a withdrawal before it was sent; the money is back in the deposit. */
	WITHDRAWAL_CANCELLED

}
