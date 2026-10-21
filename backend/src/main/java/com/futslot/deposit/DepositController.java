package com.futslot.deposit;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;
import com.futslot.common.Words;
import com.futslot.payment.PaymentResponse;
import com.futslot.payment.PlayerPayments;

import jakarta.validation.Valid;

/** A player's own booking deposit, and the deposit fees a venue is owed. */
@RestController
@RequestMapping("/api/v1")
class DepositController {

	private final DepositService deposits;

	private final PayoutService payouts;

	private final PlayerPayments payments;

	/** Where players send top-ups: FutSlot's own wallet or account, as for venues paying their invoices. */
	private final String paymentInstructions;

	/** The same in Nepali, or empty. */
	private final String paymentInstructionsNepali;

	private final Words words;

	DepositController(DepositService deposits, PayoutService payouts, PlayerPayments payments, Words words,
			@Value("${futslot.subscriptions.payment-instructions:Contact FutSlot for payment details}") String paymentInstructions,
			@Value("${futslot.subscriptions.payment-instructions-ne:}") String paymentInstructionsNepali) {
		this.deposits = deposits;
		this.payouts = payouts;
		this.payments = payments;
		this.words = words;
		this.paymentInstructions = paymentInstructions;
		this.paymentInstructionsNepali = paymentInstructionsNepali;
	}

	@GetMapping("/me/deposit")
	DepositViews.Deposit deposit(@AuthenticationPrincipal Jwt jwt) {
		UUID userId = CurrentUser.id(jwt);
		return this.deposits.forPlayer(userId)
			.withTopUp(this.payments.depositTopUpBeingChecked(userId),
					(this.words.forRequest().nepali() && StringUtils.hasText(this.paymentInstructionsNepali))
							? this.paymentInstructionsNepali : this.paymentInstructions);
	}

	@PostMapping("/me/deposit/top-ups")
	@ResponseStatus(HttpStatus.CREATED)
	PaymentResponse topUp(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DepositRequests.TopUp request) {
		return this.payments.claimDepositTopUp(CurrentUser.id(jwt), request.method(), request.transactionId(),
				request.amount());
	}

	@PostMapping("/me/deposit/withdrawals")
	@ResponseStatus(HttpStatus.CREATED)
	DepositViews.Withdrawal withdraw(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody DepositRequests.Withdraw request) {
		return this.deposits.requestWithdrawal(CurrentUser.id(jwt), request.method(), request.sendTo());
	}

	@PostMapping("/me/deposit/withdrawals/{withdrawalId}/cancel")
	DepositViews.Withdrawal cancelWithdrawal(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID withdrawalId) {
		return this.deposits.cancelWithdrawal(CurrentUser.id(jwt), withdrawalId);
	}

	@GetMapping("/owner/venues/{venueId}/deposit-fees")
	DepositViews.VenueFees venueFees(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		return this.payouts.forVenue(CurrentUser.id(jwt), venueId);
	}

}
