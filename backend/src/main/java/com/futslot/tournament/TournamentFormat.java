package com.futslot.tournament;

public enum TournamentFormat {

	/** Single elimination; byes go to the top seeds when the field isn't a power of two. */
	KNOCKOUT,

	/** Everyone plays everyone once; the table decides the winner. */
	LEAGUE,

	/** Round-robin groups, then the top two of each group play a knockout. */
	GROUP_KNOCKOUT

}
