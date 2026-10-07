package com.futslot.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TopCustomerRow(UUID customerId, String name, String phone, long bookings, BigDecimal spent,
		Instant lastPlayedAt) {

}
