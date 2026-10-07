package com.futslot.payment;

import java.time.Duration;
import java.time.Instant;

/**
 * How much of what was paid goes back. A player cancelling at least {@code freeCancelHours} before the
 * start gets everything back; later, {@code lateCancelRefundPct}. If the venue cancels, or the booking
 * expired unconfirmed, the player always gets everything back.
 */
final class RefundPolicy {

	private RefundPolicy() {
	}

	static int refundPercent(boolean cancelledByPlayer, Instant cancelledAt, Instant startsAt, int freeCancelHours,
			int lateCancelRefundPct) {
		if (!cancelledByPlayer) {
			return 100;
		}
		boolean inTime = !Duration.between(cancelledAt, startsAt).minusHours(freeCancelHours).isNegative();
		return inTime ? 100 : lateCancelRefundPct;
	}

}
