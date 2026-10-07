package com.futslot.venue;

import java.util.Set;

/** A venue the caller manages; {@code access} is OWNER, MANAGER or RECEPTIONIST. */
public record MyVenueResponse(VenueSummaryResponse venue, String access, Set<VenuePermission> permissions) {

}
