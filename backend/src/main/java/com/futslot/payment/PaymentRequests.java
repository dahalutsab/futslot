package com.futslot.payment;

import java.math.BigDecimal;

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

	record Reject(@NotBlank @Size(max = 500) String reason) {

	}

	record CompleteRefund(@Size(max = 100) String reference) {

	}

}
