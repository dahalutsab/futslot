package com.futslot.venue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.ApiException;

/**
 * The single place that decides who may act on a venue: its owner (every permission) or active staff
 * (the permissions on their record). Non-members get 404 so venue ids can't be probed; members lacking
 * a permission get 403.
 */
@Component
public class VenueAccess {

	private final VenueRepository venues;

	private final VenueStaffRepository staff;

	VenueAccess(VenueRepository venues, VenueStaffRepository staff) {
		this.venues = venues;
		this.staff = staff;
	}

	/** The venue, if {@code userId} may do {@code permission} there; null permission = any member. */
	@Transactional(readOnly = true)
	public Venue require(UUID userId, UUID venueId, VenuePermission permission) {
		Venue venue = this.venues.findById(venueId).orElseThrow(VenueService::venueNotFound);
		if (venue.getOwnerId().equals(userId)) {
			return venue;
		}
		VenueStaff member = this.staff.findByVenueIdAndUserId(venueId, userId)
			.filter(VenueStaff::isActive)
			.orElseThrow(VenueService::venueNotFound);
		if (permission != null && !member.has(permission)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "PERMISSION_DENIED",
					"You don't have permission to do this at " + venue.getName());
		}
		return venue;
	}

	/** The venue, if {@code userId} owns it: billing and other owner-only matters. Staff get 403. */
	@Transactional(readOnly = true)
	public Venue requireOwner(UUID userId, UUID venueId) {
		Venue venue = require(userId, venueId, null);
		if (!venue.getOwnerId().equals(userId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "OWNER_ONLY", "Only the owner of " + venue.getName() + " can do this");
		}
		return venue;
	}

	/** Everyone who should hear about activity needing {@code permission}: owner first, then staff. */
	@Transactional(readOnly = true)
	public List<UUID> usersWith(UUID venueId, VenuePermission permission) {
		Set<UUID> users = new LinkedHashSet<>();
		this.venues.findById(venueId).ifPresent(venue -> users.add(venue.getOwnerId()));
		this.staff.findByVenueIdAndActiveTrue(venueId)
			.stream()
			.filter(member -> member.has(permission))
			.forEach(member -> users.add(member.getUserId()));
		return new ArrayList<>(users);
	}

	/** Active staff accounts, for plan limits. */
	@Transactional(readOnly = true)
	public long activeStaff(UUID venueId) {
		return this.staff.findByVenueIdAndActiveTrue(venueId).size();
	}

	/** Every venue the user owns or works at, with what they can do there. */
	@Transactional(readOnly = true)
	public List<Membership> memberships(UUID userId) {
		List<Membership> result = new ArrayList<>();
		this.venues.findByOwnerIdOrderByCreatedAt(userId)
			.forEach(venue -> result.add(new Membership(venue, null, EnumSet.allOf(VenuePermission.class))));
		for (VenueStaff member : this.staff.findByUserIdAndActiveTrue(userId)) {
			this.venues.findById(member.getVenueId())
				.ifPresent(venue -> result.add(new Membership(venue, member.getRole(), member.getPermissions())));
		}
		return result;
	}

	/** {@code staffRole} is null for the owner. */
	public record Membership(Venue venue, StaffRole staffRole, Set<VenuePermission> permissions) {

	}

}
