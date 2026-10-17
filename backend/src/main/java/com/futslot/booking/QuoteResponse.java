package com.futslot.booking;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * {@code total = price + addOns - discount}; {@code advanceRequired} is what must be paid to confirm.
 * {@code strikeAdvance}: the advance is higher than usual because of the player's recent late cancellations
 * and no-shows. {@code depositRequired}: the venue needs a full FutSlot booking deposit; {@code depositToAdd} is
 * what the player must add to it before booking (0 when nothing is needed).
 */
public record QuoteResponse(BigDecimal price, BigDecimal addOns, BigDecimal discount, BigDecimal total,
		BigDecimal advanceRequired, boolean strikeAdvance, boolean depositRequired, BigDecimal depositToAdd,
		UUID membershipId, String discountCode) {

}
