package com.futslot.split;

/**
 * How a booking's cost is shared. "Court" is the booking total; "extras" (water, drinks) are added on
 * top. A draw always splits everything equally.
 */
public enum SplitRule {

	/** Everyone pays the same, whoever wins. */
	EQUAL,

	/** Baazi: the losing side pays everything, the winners nothing. */
	LOSERS_PAY_ALL,

	/** Baazi: the losing side pays {@code losersPercent} (e.g. 70), the winners the rest. */
	LOSERS_PAY_PERCENT,

	/** Baazi: the losing side pays for the court, the winners pay for the extras. */
	LOSERS_PAY_COURT

}
