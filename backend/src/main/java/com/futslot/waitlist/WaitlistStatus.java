package com.futslot.waitlist;

public enum WaitlistStatus {

	/** Waiting for the slot to free up. */
	WAITING,
	/** Told the slot is free; still listed in case it's taken again and frees once more. */
	NOTIFIED,
	/** They got the slot. */
	BOOKED,
	/** The slot's time passed. */
	EXPIRED,
	/** They left the waitlist. */
	CANCELLED

}
