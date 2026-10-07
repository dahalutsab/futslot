package com.futslot.booking;

public enum BookingSource {

	APP, WEBSITE, WALK_IN, PHONE, RECURRING, TOURNAMENT, ADMIN;

	/** Made by the player themselves (in the app or on the venue's website), not by staff. */
	public boolean byPlayer() {
		return this == APP || this == WEBSITE;
	}

}
