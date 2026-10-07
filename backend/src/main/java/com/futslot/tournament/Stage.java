package com.futslot.tournament;

public enum Stage {

	GROUP, LEAGUE, ROUND_OF_32, ROUND_OF_16, QUARTER_FINAL, SEMI_FINAL, THIRD_PLACE, FINAL;

	boolean isKnockout() {
		return this != GROUP && this != LEAGUE;
	}

}
