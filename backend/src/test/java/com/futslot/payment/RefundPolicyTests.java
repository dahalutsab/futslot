package com.futslot.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class RefundPolicyTests {

	@Test
	void noFeeRefundsEverything() {
		assertThat(RefundPolicy.refundAmount(new BigDecimal("600.00"), new BigDecimal("2000.00"), 0))
			.isEqualByComparingTo("600.00");
	}

	@Test
	void feeIsAShareOfTheBookingPrice() {
		assertThat(RefundPolicy.refundAmount(new BigDecimal("2000.00"), new BigDecimal("2000.00"), 50))
			.isEqualByComparingTo("1000.00");
		assertThat(RefundPolicy.refundAmount(new BigDecimal("1000.00"), new BigDecimal("1500.00"), 20))
			.isEqualByComparingTo("700.00");
	}

	@Test
	void feeNeverTakesMoreThanWasPaid() {
		assertThat(RefundPolicy.refundAmount(new BigDecimal("600.00"), new BigDecimal("2000.00"), 50)).isZero();
		assertThat(RefundPolicy.refundAmount(new BigDecimal("600.00"), new BigDecimal("2000.00"), 100)).isZero();
	}

}
