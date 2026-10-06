package com.futslot.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@SecurityRequirements // public: no bearer token needed
class AuthController {

	private final AuthService authService;

	AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/otp/request")
	@ResponseStatus(HttpStatus.ACCEPTED)
	OtpSentResponse requestOtp(@Valid @RequestBody OtpRequest request) {
		return this.authService.requestOtp(request);
	}

	@PostMapping("/otp/verify")
	TokenResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
		return this.authService.verifyOtp(request);
	}

	@PostMapping("/refresh")
	TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
		return this.authService.refresh(request);
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logout(@Valid @RequestBody RefreshRequest request) {
		this.authService.logout(request);
	}

}
