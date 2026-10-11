package com.futslot.booking;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * {@code total = price + addOns - discount}; {@code advanceRequired} is what must be paid to confirm.
 * {@code strikeAdvance}: the advance is higher than usual because of the player's recent late cancellations
 * and no-shows.
 */
public record QuoteResponse(BigDecimal price, BigDecimal addOns, BigDecimal discount, BigDecimal total,
		BigDecimal advanceRequired, boolean strikeAdvance,
		UUID membershipId, String discountCode) {

}
