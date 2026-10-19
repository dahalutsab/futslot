package com.futslot.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;

import jakarta.validation.Valid;

@RestController
class PasswordController {

	private final AuthService authService;

	PasswordController(AuthService authService) {
		this.authService = authService;
	}

	/** Changes (or first sets) the password. Every device is signed out; the new tokens keep this one in. */
	@PutMapping("/api/v1/me/password")
	TokenResponse change(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
		return this.authService.changePassword(CurrentUser.id(jwt), request);
	}

}
