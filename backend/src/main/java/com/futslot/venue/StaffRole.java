package com.futslot.venue;

import java.util.EnumSet;
import java.util.Set;

public enum StaffRole {

	MANAGER(EnumSet.of(VenuePermission.MANAGE_VENUE, VenuePermission.MANAGE_BOOKINGS, VenuePermission.MANAGE_BLOCKS,
			VenuePermission.VIEW_CUSTOMERS, VenuePermission.VIEW_REPORTS)),

	RECEPTIONIST(EnumSet.of(VenuePermission.MANAGE_BOOKINGS, VenuePermission.MANAGE_BLOCKS,
			VenuePermission.VIEW_CUSTOMERS));

	private final Set<VenuePermission> defaultPermissions;

	StaffRole(Set<VenuePermission> defaultPermissions) {
		this.defaultPermissions = defaultPermissions;
	}

	public Set<VenuePermission> defaultPermissions() {
		return EnumSet.copyOf(this.defaultPermissions);
	}

}
