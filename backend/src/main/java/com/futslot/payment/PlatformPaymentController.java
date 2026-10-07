package com.futslot.payment;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;

import jakarta.validation.Valid;

/** Venue owners report payments to FutSlot; admins verify them. */
@RestController
@RequestMapping("/api/v1")
class PlatformPaymentController {

	private final PlatformPaymentService platformPayments;

	PlatformPaymentController(PlatformPaymentService platformPayments) {
		this.platformPayments = platformPayments;
	}

	@PostMapping("/invoices/{invoiceId}/payments")
	@ResponseStatus(HttpStatus.CREATED)
	PaymentResponse payInvoice(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID invoiceId,
			@Valid @RequestBody PaymentRequests.Claim request) {
		return this.platformPayments.claimInvoice(CurrentUser.id(jwt), invoiceId, request);
	}

	@PostMapping("/featured-listings/{listingId}/payments")
	@ResponseStatus(HttpStatus.CREATED)
	PaymentResponse payListing(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listingId,
			@Valid @RequestBody PaymentRequests.Claim request) {
		return this.platformPayments.claimListing(CurrentUser.id(jwt), listingId, request);
	}

	@GetMapping("/admin/payments/pending")
	@PreAuthorize("hasRole('ADMIN')")
	List<PaymentResponse> pending() {
		return this.platformPayments.pending();
	}

	@PostMapping("/admin/payments/{paymentId}/approve")
	@PreAuthorize("hasRole('ADMIN')")
	PaymentResponse approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId) {
		return this.platformPayments.approve(CurrentUser.id(jwt), paymentId);
	}

	@PostMapping("/admin/payments/{paymentId}/reject")
	@PreAuthorize("hasRole('ADMIN')")
	PaymentResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId,
			@Valid @RequestBody PaymentRequests.Reject request) {
		return this.platformPayments.reject(CurrentUser.id(jwt), paymentId, request.reason());
	}

	@PostMapping("/admin/invoices/{invoiceId}/payments")
	@PreAuthorize("hasRole('ADMIN')")
	@ResponseStatus(HttpStatus.CREATED)
	PaymentResponse recordInvoice(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID invoiceId,
			@Valid @RequestBody PaymentRequests.RecordEntry request) {
		return this.platformPayments.recordInvoice(CurrentUser.id(jwt), invoiceId, request);
	}

	@PostMapping("/admin/featured-listings/{listingId}/payments")
	@PreAuthorize("hasRole('ADMIN')")
	@ResponseStatus(HttpStatus.CREATED)
	PaymentResponse recordListing(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listingId,
			@Valid @RequestBody PaymentRequests.RecordEntry request) {
		return this.platformPayments.recordListing(CurrentUser.id(jwt), listingId, request);
	}

}
