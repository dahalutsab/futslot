package com.futslot.payment;

import java.math.BigDecimal;
import java.util.UUID;

/** A venue's payment to FutSlot wasn't found; {@code what} is e.g. "your INV-... invoice". */
public record PlatformPaymentRejected(UUID payerUserId, BigDecimal amount, String what, String reason) {

}
