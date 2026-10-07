package com.futslot.payment;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

final class PaymentRequests {

	private PaymentRequests() {
	}

	/** A player paid the venue's wallet/QR and reports the transaction. */
	record Claim(@NotNull PaymentMethod method, @NotBlank @Size(max = 100) String transactionId,
			@NotNull @DecimalMin("1") @Digits(integer = 10, fraction = 2) BigDecimal amount) {

	}

	/** Staff record money received. {@code transactionId} is optional for cash. */
	record Record(@NotNull PaymentMethod method, @Size(max = 100) String transactionId,
			@NotNull @DecimalMin("1") @Digits(integer = 10, fraction = 2) BigDecimal amount) {

	}

	/** A player buying a plan; payment details are needed unless the plan is free. */
	record BuyMembership(@NotNull UUID planId, PaymentMethod method, @Size(max = 100) String transactionId,
			@DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal amount) {

	}

	/** Staff selling a plan at the counter; the plan's price is taken as paid. */
	record SellMembership(@NotNull UUID planId, @NotBlank @Size(max = 100) String customerName,
			@NotBlank String customerPhone, PaymentMethod method, @Size(max = 100) String transactionId) {

	}

	/** Staff recording an entry fee paid at the venue; the tournament's fee is taken as the amount. */
	record RecordEntry(@NotNull PaymentMethod method, @Size(max = 100) String transactionId) {

	}

	record Reject(@NotBlank @Size(max = 500) String reason) {

	}

	record CompleteRefund(@Size(max = 100) String reference) {

	}

}
