package com.futslot.deposit;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;

import jakarta.validation.Valid;

/**
 * FutSlot admins sending deposit money out: deposits players asked for back, and monthly fee payouts to venues.
 * Top-ups are checked with the other payments to FutSlot, under /admin/payments.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
class DepositAdminController {

	private final DepositService deposits;

	private final PayoutService payouts;

	DepositAdminController(DepositService deposits, PayoutService payouts) {
		this.deposits = deposits;
		this.payouts = payouts;
	}

	/** Soonest due first. */
	@GetMapping("/deposit-withdrawals")
	List<DepositViews.Withdrawal> withdrawals(
			@RequestParam(defaultValue = "REQUESTED") WithdrawalStatus status) {
		return this.deposits.withdrawals(status);
	}

	@PostMapping("/deposit-withdrawals/{withdrawalId}/paid")
	DepositViews.Withdrawal withdrawalPaid(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID withdrawalId,
			@Valid @RequestBody DepositRequests.MarkWithdrawalPaid request) {
		return this.deposits.markWithdrawalPaid(CurrentUser.id(jwt), withdrawalId, request.reference());
	}

	@GetMapping("/payouts")
	List<DepositViews.Payout> payouts() {
		return this.payouts.pending();
	}

	@PostMapping("/payouts/{payoutId}/paid")
	DepositViews.Payout payoutPaid(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID payoutId,
			@Valid @RequestBody DepositRequests.MarkPayoutPaid request) {
		return this.payouts.markPaid(CurrentUser.id(jwt), payoutId, request);
	}

}
