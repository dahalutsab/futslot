package com.futslot.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Published after billing changes for a venue owner; consumed by notifications after commit. */
public record BillingEvent(Type type, UUID venueId, String venueName, UUID ownerId, String planName,
		BigDecimal amount, LocalDate date) {

	public enum Type {

		/** An invoice was raised; {@code date} is when it's due. */
		INVOICE_DUE,
		/** The plan is paid for; {@code date} is when the paid period ends. */
		SUBSCRIPTION_ACTIVE,
		/** The period ended unpaid; {@code date} is the end of the grace period. */
		SUBSCRIPTION_PAST_DUE,
		/** Back on the free plan. */
		SUBSCRIPTION_ENDED,
		/** A featured listing is paid for; {@code date} is its first day. */
		FEATURED_CONFIRMED

	}

}
