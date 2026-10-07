package com.futslot.payment;

import java.math.BigDecimal;
import java.util.UUID;

import com.futslot.tournament.Registration;

/** Payment activity on a tournament entry fee; consumed by notifications after commit. */
public record EntryPaymentEvent(PaymentEvent.Type type, UUID registrationId, UUID venueId, String tournamentName,
		String teamName, UUID registeredBy, BigDecimal amount, PaymentMethod method, String reason) {

	static EntryPaymentEvent of(PaymentEvent.Type type, Registration entry, BigDecimal amount, PaymentMethod method,
			String reason) {
		return new EntryPaymentEvent(type, entry.getId(), entry.getTournament().getVenue().getId(),
				entry.getTournament().getName(), entry.getTeam().getName(), entry.getRegisteredBy(), amount, method,
				reason);
	}

}
