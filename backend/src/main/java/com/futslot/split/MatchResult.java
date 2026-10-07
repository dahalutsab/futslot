package com.futslot.split;

public enum MatchResult {

	A_WON, B_WON, DRAW;

	/** The losing side, or null for a draw. */
	Side loser() {
		return switch (this) {
			case A_WON -> Side.B;
			case B_WON -> Side.A;
			case DRAW -> null;
		};
	}

}
