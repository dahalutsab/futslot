package com.futslot.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.booking.Booking;
import com.futslot.booking.BookingEvent;
import com.futslot.booking.BookingEventType;
import com.futslot.booking.BookingLookup;
import com.futslot.booking.BookingStatus;
import com.futslot.common.ApiException;
import com.futslot.venue.VenueAccess;
import com.futslot.venue.VenuePermission;
import com.futslot.venue.VenuePolicy;

/**
 * Payments are made to the venue directly (cash, or the venue's own eSewa/Khalti/Fonepay/bank). Players
 * report a wallet payment and staff verify it; staff record cash. FutSlot tracks what was paid and what
 * is owed back, but never holds the money. A gateway checkout can later create SUCCEEDED payments the
 * same way.
 */
@Service
class PaymentService {

	/** Methods a player can report; cash is only recorded by staff. */
	private static final Set<PaymentMethod> CLAIMABLE = EnumSet.of(PaymentMethod.ESEWA, PaymentMethod.KHALTI,
			PaymentMethod.FONEPAY, PaymentMethod.BANK_TRANSFER);

	private static final Set<PaymentMethod> RECORDABLE = EnumSet.of(PaymentMethod.CASH, PaymentMethod.ESEWA,
			PaymentMethod.KHALTI, PaymentMethod.FONEPAY, PaymentMethod.BANK_TRANSFER);

	private static final Set<TransactionStatus> REFUNDABLE = EnumSet.of(TransactionStatus.SUCCEEDED,
			TransactionStatus.PARTIALLY_REFUNDED);

	private final PaymentRepository payments;

	private final RefundRepository refunds;

	private final BookingLookup bookings;

	private final VenueAccess access;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	PaymentService(PaymentRepository payments, RefundRepository refunds, BookingLookup bookings, VenueAccess access,
			ApplicationEventPublisher events, Clock clock) {
		this.payments = payments;
		this.refunds = refunds;
		this.bookings = bookings;
		this.access = access;
		this.events = events;
		this.clock = clock;
	}

	@Transactional
	PaymentResponse claim(UUID userId, UUID bookingId, PaymentRequests.Claim request) {
		Booking booking = this.bookings.forPlayer(userId, bookingId);
		if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
			throw new ApiException(HttpStatus.CONFLICT, "NO_PAYMENT_DUE", "This booking isn't waiting for a payment");
		}
		if (this.payments.existsByBookingIdAndStatus(bookingId, TransactionStatus.PENDING)) {
			throw new ApiException(HttpStatus.CONFLICT, "PAYMENT_PENDING", "Your payment is already being verified");
		}
		if (booking.getHoldExpiresAt() == null || !booking.getHoldExpiresAt().isAfter(this.clock.instant())) {
			throw new ApiException(HttpStatus.CONFLICT, "HOLD_EXPIRED", "The time to pay has run out. Book again");
		}
		if (!CLAIMABLE.contains(request.method())) {
			throw badRequest("INVALID_METHOD", "Pay with eSewa, Khalti, Fonepay or bank transfer");
		}
		BigDecimal minimum = booking.getAdvanceRequired().subtract(booking.getPaidAmount()).max(BigDecimal.ZERO);
		if (request.amount().compareTo(minimum) < 0) {
			throw badRequest("AMOUNT_TOO_LOW", "Pay at least Rs " + minimum.toPlainString() + " to confirm");
		}
		validateAmount(booking, request.amount());
		String transactionId = uniqueTransactionId(request.method(), request.transactionId());

		Payment payment = this.payments.save(Payment.claimed(booking, userId, request.amount(), request.method(),
				transactionId));
		booking.pauseHold();
		this.events.publishEvent(PaymentEvent.of(PaymentEvent.Type.SUBMITTED, booking, request.amount(), request.method(),
				null, userId));
		return PaymentResponse.from(payment);
	}

	@Transactional
	PaymentResponse record(UUID userId, UUID venueId, UUID bookingId, PaymentRequests.Record request) {
		Booking booking = this.bookings.forVenue(userId, venueId, bookingId);
		if (!booking.isLive() && booking.getStatus() != BookingStatus.COMPLETED) {
			throw new ApiException(HttpStatus.CONFLICT, "BOOKING_NOT_ACTIVE",
					"Can't take payment for a booking that is " + booking.getStatus());
		}
		if (!RECORDABLE.contains(request.method())) {
			throw badRequest("INVALID_METHOD", "Use cash, eSewa, Khalti, Fonepay or bank transfer");
		}
		validateAmount(booking, request.amount());
		String transactionId = (request.transactionId() != null && !request.transactionId().isBlank())
				? uniqueTransactionId(request.method(), request.transactionId()) : null;
		Payment payment = this.payments.save(Payment.received(booking, request.amount(), request.method(),
				transactionId, userId, this.clock.instant()));
		booking.recordPayment(request.amount());
		this.events.publishEvent(PaymentEvent.of(PaymentEvent.Type.RECEIVED, booking, request.amount(), request.method(),
				null, userId));
		return PaymentResponse.from(payment);
	}

	@Transactional
	PaymentResponse approve(UUID userId, UUID venueId, UUID paymentId) {
		Payment payment = pendingAtVenue(userId, venueId, paymentId);
		Booking booking = payment.getBooking();
		if (!booking.isLive()) {
			throw new ApiException(HttpStatus.CONFLICT, "BOOKING_NOT_ACTIVE",
					"The booking is " + booking.getStatus() + "; reject this payment and refund it instead");
		}
		payment.approve(userId, this.clock.instant());
		booking.recordPayment(payment.getAmount());
		this.events.publishEvent(PaymentEvent.of(PaymentEvent.Type.RECEIVED, booking, payment.getAmount(),
				payment.getMethod(), null, userId));
		return PaymentResponse.from(payment);
	}

	/** The money wasn't found: the player gets the venue's normal hold time again to pay. */
	@Transactional
	PaymentResponse reject(UUID userId, UUID venueId, UUID paymentId, String reason) {
		Payment payment = pendingAtVenue(userId, venueId, paymentId);
		Booking booking = payment.getBooking();
		payment.reject(userId, reason.strip());
		VenuePolicy policy = booking.getVenue().policy();
		booking.resumeHold(this.clock.instant().plus(Duration.ofMinutes(policy.bookingHoldMinutes())));
		this.events.publishEvent(PaymentEvent.of(PaymentEvent.Type.REJECTED, booking, payment.getAmount(),
				payment.getMethod(), reason.strip(), userId));
		return PaymentResponse.from(payment);
	}

	@Transactional(readOnly = true)
	List<PaymentResponse> pending(UUID userId, UUID venueId) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_BOOKINGS);
		return this.payments.findByVenueIdAndStatusOrderByCreatedAt(venueId, TransactionStatus.PENDING)
			.stream()
			.map(PaymentResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	BookingPaymentsResponse forPlayer(UUID userId, UUID bookingId) {
		return summary(this.bookings.forPlayer(userId, bookingId));
	}

	@Transactional(readOnly = true)
	BookingPaymentsResponse forVenue(UUID userId, UUID venueId, UUID bookingId) {
		return summary(this.bookings.forVenue(userId, venueId, bookingId));
	}

	@Transactional(readOnly = true)
	List<RefundResponse> refunds(UUID userId, UUID venueId, RefundStatus status) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_BOOKINGS);
		return this.refunds.findForVenue(venueId, status).stream().map(RefundResponse::from).toList();
	}

	@Transactional
	RefundResponse completeRefund(UUID userId, UUID venueId, UUID refundId, String reference) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_BOOKINGS);
		Refund refund = this.refunds.findForVenue(refundId, venueId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "REFUND_NOT_FOUND", "Refund not found"));
		if (refund.getStatus() != RefundStatus.REQUESTED) {
			throw new ApiException(HttpStatus.CONFLICT, "INVALID_REFUND_STATUS", "This refund is " + refund.getStatus());
		}
		Instant now = this.clock.instant();
		refund.complete(userId, (reference != null && !reference.isBlank()) ? reference.strip() : null, now);
		Payment payment = refund.getPayment();
		BigDecimal refundedSoFar = this.refunds.findForBooking(payment.getBooking().getId())
			.stream()
			.filter(r -> r.getPayment().getId().equals(payment.getId()) && r.getStatus() == RefundStatus.SUCCEEDED)
			.map(Refund::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		payment.refunded(refundedSoFar);
		payment.getBooking().recordRefund(refund.getAmount());
		this.events.publishEvent(PaymentEvent.of(PaymentEvent.Type.REFUNDED, payment.getBooking(), refund.getAmount(),
				payment.getMethod(), null, userId));
		return RefundResponse.from(refund);
	}

	/**
	 * Runs inside the cancelling (or expiring) transaction, so the booking and what's owed back never
	 * disagree. Unverified payments are dropped; verified money is refunded per the venue's policy.
	 */
	@EventListener
	void onBookingEnded(BookingEvent event) {
		if (event.type() != BookingEventType.CANCELLED && event.type() != BookingEventType.EXPIRED) {
			return;
		}
		Booking booking = this.bookings.byId(event.bookingId());
		this.payments.findByBookingIdAndStatusIn(booking.getId(), Set.of(TransactionStatus.PENDING))
			.forEach(payment -> payment.cancel("Booking " + event.type().name().toLowerCase()));
		if (booking.getPaidAmount().signum() <= 0) {
			return;
		}
		Instant now = this.clock.instant();
		VenuePolicy policy = booking.getVenue().policy();
		boolean byPlayer = event.type() == BookingEventType.CANCELLED && event.actorUserId() != null
				&& event.actorUserId().equals(event.playerUserId());
		int percent = RefundPolicy.refundPercent(byPlayer, now, booking.getStartsAt(), policy.freeCancelHours(),
				policy.lateCancelRefundPct());
		BigDecimal owed = booking.getPaidAmount()
			.multiply(BigDecimal.valueOf(percent))
			.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		if (owed.signum() <= 0) {
			return;
		}
		String reason = (event.type() == BookingEventType.EXPIRED) ? "Booking expired before confirmation"
				: (byPlayer ? "Cancelled by player (" + percent + "% refund)" : "Cancelled by venue");
		BigDecimal remaining = owed;
		List<Payment> paid = this.payments.findByBookingIdAndStatusIn(booking.getId(), REFUNDABLE)
			.stream()
			.sorted(Comparator.comparing(Payment::getCreatedAt).reversed())
			.toList();
		for (Payment payment : paid) {
			BigDecimal available = payment.getAmount().subtract(this.payments.refundedOrOwed(payment.getId()));
			BigDecimal part = available.min(remaining);
			if (part.signum() > 0) {
				this.refunds.save(new Refund(payment, part, reason, event.actorUserId()));
				remaining = remaining.subtract(part);
			}
		}
		this.events.publishEvent(PaymentEvent.of(PaymentEvent.Type.REFUND_DUE, booking, owed.subtract(remaining), null,
				reason, event.actorUserId()));
	}

	private BookingPaymentsResponse summary(Booking booking) {
		List<PaymentResponse> history = this.payments.findByBookingIdOrderByCreatedAt(booking.getId())
			.stream()
			.map(PaymentResponse::from)
			.toList();
		List<RefundResponse> refundHistory = this.refunds.findForBooking(booking.getId())
			.stream()
			.map(RefundResponse::from)
			.toList();
		return new BookingPaymentsResponse(booking.getTotalAmount(), booking.getAdvanceRequired(),
				booking.getPaidAmount(), booking.amountDue(), booking.getPaymentStatus(),
				booking.getVenue().getPaymentInstructions(), history, refundHistory);
	}

	private Payment pendingAtVenue(UUID userId, UUID venueId, UUID paymentId) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_BOOKINGS);
		Payment payment = this.payments.findByIdAndVenueId(paymentId, venueId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found"));
		if (payment.getStatus() != TransactionStatus.PENDING) {
			throw new ApiException(HttpStatus.CONFLICT, "INVALID_PAYMENT_STATUS", "This payment is " + payment.getStatus());
		}
		return payment;
	}

	private static void validateAmount(Booking booking, BigDecimal amount) {
		if (amount.compareTo(booking.amountDue()) > 0) {
			throw badRequest("AMOUNT_TOO_HIGH", "Only Rs " + booking.amountDue().toPlainString() + " is due");
		}
	}

	private String uniqueTransactionId(PaymentMethod method, String transactionId) {
		String id = transactionId.strip();
		if (this.payments.existsByMethodAndTransactionId(method, id)) {
			throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_TRANSACTION",
					"This " + method + " transaction has already been used");
		}
		return id;
	}

	private static ApiException badRequest(String code, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, code, message);
	}

}
