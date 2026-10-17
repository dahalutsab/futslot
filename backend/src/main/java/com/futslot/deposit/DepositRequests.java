package com.futslot.deposit;

import java.math.BigDecimal;

import com.futslot.payment.PaymentMethod;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

final class DepositRequests {

	private DepositRequests() {
	}

	/** The player sent FutSlot money to fill their deposit and reports the transaction. */
	record TopUp(@NotNull PaymentMethod method, @NotBlank @Size(max = 100) String transactionId,
			@NotNull @DecimalMin("1") @Digits(integer = 10, fraction = 2) BigDecimal amount) {

	}

	/** {@code sendTo}: the eSewa or Khalti number, or bank account, to send the deposit back to. */
	record Withdraw(@NotNull PaymentMethod method, @NotBlank @Size(max = 200) String sendTo) {

	}

	/** {@code reference}: the transfer's transaction id or note. */
	record MarkWithdrawalPaid(@NotBlank @Size(max = 100) String reference) {

	}

	record MarkPayoutPaid(@NotNull PaymentMethod method, @NotBlank @Size(max = 100) String reference) {

	}

}
