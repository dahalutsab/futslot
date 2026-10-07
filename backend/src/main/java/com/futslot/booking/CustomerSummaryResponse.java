package com.futslot.booking;

import java.time.Instant;
import java.util.UUID;

/** {@code hasApp}: the customer is linked to a FutSlot account and gets notifications. */
public record CustomerSummaryResponse(UUID id, String name, String phone, boolean hasApp, int totalBookings,
		Instant lastBookingAt) {

	static CustomerSummaryResponse from(VenueCustomer customer) {
		return new CustomerSummaryResponse(customer.getId(), customer.getName(), customer.getPhone(),
				customer.getUserId() != null, customer.getTotalBookings(), customer.getLastBookingAt());
	}

}
