package com.futslot.billing;

public enum SubscriptionStatus {

	/** Not used yet: free trials are off. */
	TRIALING,
	/** Paid up for the current period. */
	ACTIVE,
	/** Waiting for payment: the first invoice, or a renewal after the period ended (grace period). */
	PAST_DUE,
	/** Stopped by the owner, or switched to the free plan; the venue is back on Free. */
	CANCELLED,
	/** Never paid, or the renewal wasn't paid in the grace period; the venue is back on Free. */
	EXPIRED;

	boolean isLive() {
		return this == TRIALING || this == ACTIVE || this == PAST_DUE;
	}

}
