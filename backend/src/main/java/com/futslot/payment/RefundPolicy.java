package com.futslot.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * How much of what was paid goes back. A late cancellation costs the player the venue's fee share of the
 * booking price, but never more than they paid; the rest is refunded. With no fee (the venue cancelled, the
 * booking expired, or the player cancelled in time) everything goes back.
 */
final class RefundPolicy {

	private RefundPolicy() {
	}

	static BigDecimal refundAmount(BigDecimal paid, BigDecimal bookingTotal, int feePercent) {
		BigDecimal fee = bookingTotal.multiply(BigDecimal.valueOf(feePercent))
			.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
			.min(paid);
		return paid.subtract(fee);
	}

}
