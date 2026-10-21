package com.futslot.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A payment to FutSlot wasn't found; {@code what} is e.g. "your INV-... invoice". {@code deposit}: it was a
 * player's booking deposit (otherwise a venue's bill).
 */
public record PlatformPaymentRejected(UUID payerUserId, BigDecimal amount, String what, String reason, boolean deposit) {

}
