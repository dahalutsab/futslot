package com.futslot.tournament;

public enum MatchStatus {

	SCHEDULED, IN_PROGRESS, COMPLETED, WALKOVER, POSTPONED, CANCELLED;

	boolean isDecided() {
		return this == COMPLETED || this == WALKOVER;
	}

}
