package com.futslot.tournament;

public enum RegistrationStatus {

	PENDING_PAYMENT, CONFIRMED, WAITLISTED, WITHDRAWN, REJECTED;

	/** Holds one of the tournament's places. */
	boolean holdsPlace() {
		return this == PENDING_PAYMENT || this == CONFIRMED;
	}

}
