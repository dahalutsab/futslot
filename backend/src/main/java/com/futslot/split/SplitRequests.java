package com.futslot.split;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

final class SplitRequests {

	private SplitRequests() {
	}

	/**
	 * {@code losersPercent} (50-100) is needed for LOSERS_PAY_PERCENT; {@code extrasAmount} is water,
	 * drinks and so on, paid on top of the court. {@code bookerSide} puts the booker on a side when the
	 * split is first set up.
	 */
	record Rules(@NotNull SplitRule rule, @Min(50) @Max(100) Integer losersPercent,
			@DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal extrasAmount,
			@Size(max = 100) String extrasNote, @Size(max = 30) String sideAName, @Size(max = 30) String sideBName,
			Side bookerSide) {

	}

	/** An app user by phone, or a guest by name (phone optional). */
	record AddPlayer(@Size(max = 60) String name, String phone, @NotNull Side side) {

	}

	record MovePlayer(@NotNull Side side) {

	}

	/** Not needed for an EQUAL split. */
	record Result(MatchResult result) {

	}

}
