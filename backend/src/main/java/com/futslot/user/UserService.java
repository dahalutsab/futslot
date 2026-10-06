package com.futslot.user;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.ApiException;

@Service
public class UserService {

	private final UserRepository users;

	public UserService(UserRepository users) {
		this.users = users;
	}

	@Transactional(readOnly = true)
	public User get(UUID id) {
		return this.users.findById(id)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
	}

	@Transactional
	public User updateProfile(UUID id, UpdateProfileRequest request) {
		User user = get(id);
		if (request.fullName() != null) {
			user.setFullName(request.fullName().strip());
		}
		if (request.email() != null) {
			String email = request.email().strip();
			if (this.users.existsByEmailIgnoreCaseAndIdNot(email, id)) {
				throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "That email is already in use");
			}
			user.setEmail(email);
		}
		if (request.preferredLanguage() != null) {
			user.setPreferredLanguage(request.preferredLanguage());
		}
		return user;
	}

}
