package com.futslot.booking;

import java.math.BigDecimal;
import java.util.UUID;

/** {@code total = price - discount}; {@code advanceRequired} is what must be paid to confirm. */
public record QuoteResponse(BigDecimal price, BigDecimal discount, BigDecimal total, BigDecimal advanceRequired,
		UUID membershipId, String discountCode) {

}
