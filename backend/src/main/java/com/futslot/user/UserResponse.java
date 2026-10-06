package com.futslot.user;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(UUID id, String fullName, String phone, String email, String avatarUrl,
		String preferredLanguage, Set<Role> roles, Instant createdAt) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getFullName(), user.getPhone(), user.getEmail(),
				user.getAvatarUrl(), user.getPreferredLanguage(), Set.copyOf(user.getRoles()), user.getCreatedAt());
	}

}
