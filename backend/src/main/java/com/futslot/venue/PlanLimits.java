package com.futslot.venue;

import java.util.UUID;

/**
 * What a venue's subscription plan allows. Implemented by billing; a no-op until limits are switched
 * on (futslot.subscriptions.enforce-limits).
 */
public interface PlanLimits {

	/** Before adding a court to a venue that has {@code courts} already. */
	void checkCourts(UUID venueId, long courts);

	/** Before adding a staff member to a venue that has {@code staff} active already. */
	void checkStaff(UUID venueId, long staff);

}
