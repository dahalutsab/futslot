package com.futslot.venue;

/** What a venue member may do. Owners have every permission; staff have what their record lists. */
public enum VenuePermission {

	/** Venue details, policy, hours, amenities, courts and pricing. */
	MANAGE_VENUE,

	/** Create, move, cancel bookings and mark attendance. */
	MANAGE_BOOKINGS,

	/** Block and unblock court time. */
	MANAGE_BLOCKS,

	/** See and edit the customer list. */
	VIEW_CUSTOMERS,

	/** Add, change and remove staff. Owner only; cannot be granted to staff. */
	MANAGE_STAFF

}
