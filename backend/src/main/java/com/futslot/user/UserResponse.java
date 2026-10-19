package com.futslot.user;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** {@code passwordSet} is false for accounts made before passwords, or with Google, until one is set. */
public record UserResponse(UUID id, String fullName, String phone, String email, String avatarUrl,
		String preferredLanguage, Set<Role> roles, boolean passwordSet, boolean googleLinked, Instant createdAt) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getFullName(), user.getPhone(), user.getEmail(),
				user.getAvatarUrl(), user.getPreferredLanguage(), Set.copyOf(user.getRoles()), user.hasPassword(),
				user.getGoogleSubject() != null, user.getCreatedAt());
	}

}
