package com.futslot.payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.futslot.membership.Membership;

/** Payment activity on a membership; consumed by notifications after commit. Types as in {@link PaymentEvent}. */
public record MembershipPaymentEvent(PaymentEvent.Type type, UUID membershipId, UUID venueId, String venueName,
		String planName, String customerName, UUID playerUserId, BigDecimal amount, PaymentMethod method,
		String reason, LocalDate expiresOn, UUID actorUserId) {

	static MembershipPaymentEvent of(PaymentEvent.Type type, Membership membership, BigDecimal amount,
			PaymentMethod method, String reason, UUID actorUserId) {
		return new MembershipPaymentEvent(type, membership.getId(), membership.getVenue().getId(),
				membership.getVenue().getName(), membership.getPlan().getName(), membership.getCustomer().getName(),
				membership.getCustomer().getUserId(), amount, method, reason, membership.getExpiresOn(), actorUserId);
	}

}
