package com.futslot.booking;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/** Short codes players can read out at the counter, e.g. FS-7KQ2MX. No 0/O, 1/I/L to avoid mix-ups. */
@Component
class BookingReferences {

	private static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";

	private final SecureRandom random = new SecureRandom();

	private final BookingRepository bookings;

	BookingReferences(BookingRepository bookings) {
		this.bookings = bookings;
	}

	String next() {
		String reference;
		do {
			StringBuilder code = new StringBuilder("FS-");
			for (int i = 0; i < 6; i++) {
				code.append(ALPHABET.charAt(this.random.nextInt(ALPHABET.length())));
			}
			reference = code.toString();
		}
		while (this.bookings.existsByReference(reference));
		return reference;
	}

}
