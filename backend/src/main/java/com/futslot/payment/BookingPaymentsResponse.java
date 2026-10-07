package com.futslot.payment;

import java.math.BigDecimal;
import java.util.List;

import com.futslot.booking.PaymentStatus;

/** Everything about the money side of one booking, including how to pay the venue. */
public record BookingPaymentsResponse(BigDecimal totalAmount, BigDecimal advanceRequired, BigDecimal paidAmount,
		BigDecimal amountDue, PaymentStatus paymentStatus, String paymentInstructions, List<PaymentResponse> payments,
		List<RefundResponse> refunds) {

}
