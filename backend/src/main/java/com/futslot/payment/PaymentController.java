package com.futslot.payment;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;
import com.futslot.membership.MembershipResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
class PaymentController {

	private final PaymentService paymentService;

	PaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	// Player

	@GetMapping("/bookings/{bookingId}/payments")
	BookingPaymentsResponse myPayments(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId) {
		return this.paymentService.forPlayer(CurrentUser.id(jwt), bookingId);
	}

	/** "I paid the venue Rs X on eSewa, transaction id Y": holds the slot while staff verify. */
	@PostMapping("/bookings/{bookingId}/payments")
	@ResponseStatus(HttpStatus.CREATED)
	PaymentResponse claim(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId,
			@Valid @RequestBody PaymentRequests.Claim request) {
		return this.paymentService.claim(CurrentUser.id(jwt), bookingId, request);
	}

	/** Buy a venue's membership plan after paying the venue. */
	@PostMapping("/me/memberships")
	@ResponseStatus(HttpStatus.CREATED)
	MembershipResponse buyMembership(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody PaymentRequests.BuyMembership request) {
		return this.paymentService.buyMembership(CurrentUser.id(jwt), request);
	}

	/** Pay a tournament entry fee to the venue, then report it here. */
	@PostMapping("/tournament-entries/{registrationId}/payments")
	@ResponseStatus(HttpStatus.CREATED)
	PaymentResponse claimEntry(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID registrationId,
			@Valid @RequestBody PaymentRequests.Claim request) {
		return this.paymentService.claimEntry(CurrentUser.id(jwt), registrationId, request);
	}

	// Venue

	@PostMapping("/owner/venues/{venueId}/tournament-entries/{registrationId}/payments")
	@ResponseStatus(HttpStatus.CREATED)
	PaymentResponse recordEntry(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID registrationId, @Valid @RequestBody PaymentRequests.RecordEntry request) {
		return this.paymentService.recordEntry(CurrentUser.id(jwt), venueId, registrationId, request);
	}

	@PostMapping("/owner/venues/{venueId}/memberships")
	@ResponseStatus(HttpStatus.CREATED)
	MembershipResponse sellMembership(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@Valid @RequestBody PaymentRequests.SellMembership request) {
		return this.paymentService.sellMembership(CurrentUser.id(jwt), venueId, request);
	}

	@GetMapping("/owner/venues/{venueId}/bookings/{bookingId}/payments")
	BookingPaymentsResponse bookingPayments(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID bookingId) {
		return this.paymentService.forVenue(CurrentUser.id(jwt), venueId, bookingId);
	}

	@PostMapping("/owner/venues/{venueId}/bookings/{bookingId}/payments")
	@ResponseStatus(HttpStatus.CREATED)
	PaymentResponse record(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId, @PathVariable UUID bookingId,
			@Valid @RequestBody PaymentRequests.Record request) {
		return this.paymentService.record(CurrentUser.id(jwt), venueId, bookingId, request);
	}

	@GetMapping("/owner/venues/{venueId}/payments/pending")
	List<PaymentResponse> pending(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		return this.paymentService.pending(CurrentUser.id(jwt), venueId);
	}

	@PostMapping("/owner/venues/{venueId}/payments/{paymentId}/approve")
	PaymentResponse approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId, @PathVariable UUID paymentId) {
		return this.paymentService.approve(CurrentUser.id(jwt), venueId, paymentId);
	}

	@PostMapping("/owner/venues/{venueId}/payments/{paymentId}/reject")
	PaymentResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId, @PathVariable UUID paymentId,
			@Valid @RequestBody PaymentRequests.Reject request) {
		return this.paymentService.reject(CurrentUser.id(jwt), venueId, paymentId, request.reason());
	}

	@GetMapping("/owner/venues/{venueId}/refunds")
	List<RefundResponse> refunds(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@RequestParam(defaultValue = "REQUESTED") RefundStatus status) {
		return this.paymentService.refunds(CurrentUser.id(jwt), venueId, status);
	}

	@PostMapping("/owner/venues/{venueId}/refunds/{refundId}/complete")
	RefundResponse completeRefund(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID refundId, @Valid @RequestBody(required = false) PaymentRequests.CompleteRefund request) {
		return this.paymentService.completeRefund(CurrentUser.id(jwt), venueId, refundId,
				(request != null) ? request.reference() : null);
	}

}
