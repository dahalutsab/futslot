package com.futslot.venue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.ApiException;
import com.futslot.common.PhoneNumbers;
import com.futslot.user.Role;
import com.futslot.user.User;
import com.futslot.user.UserRepository;

/** Owners add receptionists and managers by phone; they then log in with the normal OTP flow. */
@Service
class StaffService {

	private final VenueAccess access;

	private final VenueStaffRepository staff;

	private final UserRepository users;

	private final ApplicationEventPublisher events;

	StaffService(VenueAccess access, VenueStaffRepository staff, UserRepository users, ApplicationEventPublisher events) {
		this.access = access;
		this.staff = staff;
		this.users = users;
		this.events = events;
	}

	@Transactional(readOnly = true)
	List<StaffResponse> list(UUID userId, UUID venueId) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_STAFF);
		return toResponses(this.staff.findByVenueIdOrderByCreatedAt(venueId));
	}

	@Transactional
	StaffResponse add(UUID userId, UUID venueId, StaffRequest request) {
		Venue venue = this.access.require(userId, venueId, VenuePermission.MANAGE_STAFF);
		String phone = PhoneNumbers.normalize(request.phone());
		User person = this.users.findByPhone(phone)
			.orElseGet(() -> this.users.save(new User(phone, request.fullName().strip())));
		if (person.getId().equals(venue.getOwnerId())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "ALREADY_OWNER", "That's the venue owner");
		}
		Set<VenuePermission> permissions = permissions(request.role(), request.permissions());
		VenueStaff member = this.staff.findByVenueIdAndUserId(venueId, person.getId())
			.map(existing -> {
				existing.assign(request.role(), permissions, true);
				return existing;
			})
			.orElseGet(() -> this.staff.save(new VenueStaff(venueId, person.getId(), userId, request.role(), permissions)));
		person.getRoles().add(Role.STAFF);
		this.events.publishEvent(new StaffAdded(venueId, venue.getName(), person.getId(), request.role(), userId));
		return toResponses(List.of(member)).getFirst();
	}

	@Transactional
	StaffResponse update(UUID userId, UUID venueId, UUID staffId, StaffUpdateRequest request) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_STAFF);
		VenueStaff member = member(venueId, staffId);
		member.assign(request.role(), permissions(request.role(), request.permissions()),
				!Boolean.FALSE.equals(request.active()));
		return toResponses(List.of(member)).getFirst();
	}

	@Transactional
	void remove(UUID userId, UUID venueId, UUID staffId) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_STAFF);
		VenueStaff member = member(venueId, staffId);
		member.assign(member.getRole(), member.getPermissions(), false);
	}

	private VenueStaff member(UUID venueId, UUID staffId) {
		return this.staff.findByIdAndVenueId(staffId, venueId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STAFF_NOT_FOUND", "Staff member not found"));
	}

	private static Set<VenuePermission> permissions(StaffRole role, Set<VenuePermission> requested) {
		Set<VenuePermission> permissions = (requested != null) ? requested : role.defaultPermissions();
		if (permissions.contains(VenuePermission.MANAGE_STAFF)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "PERMISSION_NOT_GRANTABLE",
					"Only the owner can manage staff");
		}
		return permissions;
	}

	private List<StaffResponse> toResponses(List<VenueStaff> members) {
		Map<UUID, User> people = this.users.findAllById(members.stream().map(VenueStaff::getUserId).toList())
			.stream()
			.collect(Collectors.toMap(User::getId, Function.identity()));
		return members.stream().map(member -> {
			User person = people.get(member.getUserId());
			return new StaffResponse(member.getId(), member.getUserId(), person.getFullName(), person.getPhone(),
					member.getRole(), member.getPermissions(), member.isActive(), member.getCreatedAt());
		}).toList();
	}

}
