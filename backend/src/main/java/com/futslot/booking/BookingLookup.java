package com.futslot.booking;

import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.futslot.common.ApiException;
import com.futslot.venue.VenueAccess;
import com.futslot.venue.VenuePermission;

/** Access-checked booking lookups for other modules (payments). Call inside a transaction. */
@Component
public class BookingLookup {

	private final BookingRepository bookings;

	private final VenueAccess access;

	BookingLookup(BookingRepository bookings, VenueAccess access) {
		this.bookings = bookings;
		this.access = access;
	}

	/** A booking of the player's (made by them or by a venue for them). */
	public Booking forPlayer(UUID userId, UUID bookingId) {
		return this.bookings.findByIdAndCustomerUserId(bookingId, userId).orElseThrow(BookingLookup::notFound);
	}

	/** A booking at a venue where the user handles bookings. */
	public Booking forVenue(UUID userId, UUID venueId, UUID bookingId) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_BOOKINGS);
		return this.bookings.findByIdAndVenueId(bookingId, venueId).orElseThrow(BookingLookup::notFound);
	}

	/** Whether the player has bookings, not yet played or cancelled, that their deposit covers. */
	public boolean hasLiveDepositBookings(UUID userId) {
		return this.bookings.existsByBookedByUserIdAndDepositBackedTrueAndStatusIn(userId,
				EnumSet.of(BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED));
	}

	/** Whether the player already has this court for (part of) this time. */
	public boolean playerHolds(UUID userId, UUID courtId, Instant from, Instant to) {
		return this.bookings.playerHolds(userId, courtId, from, to,
				EnumSet.of(BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED));
	}

	/** No access check: for reacting to events about a booking. */
	public Booking byId(UUID bookingId) {
		return this.bookings.findById(bookingId).orElseThrow(BookingLookup::notFound);
	}

	private static ApiException notFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "Booking not found");
	}

}
