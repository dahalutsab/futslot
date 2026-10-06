package com.futslot.notification;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;
import com.futslot.common.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1")
@Validated
class NotificationController {

	private final NotificationService notificationService;

	NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping("/notifications")
	PageResponse<NotificationResponse> inbox(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
		return this.notificationService.inbox(CurrentUser.id(jwt), page, size);
	}

	@GetMapping("/notifications/unread-count")
	Map<String, Long> unreadCount(@AuthenticationPrincipal Jwt jwt) {
		return Map.of("unread", this.notificationService.unreadCount(CurrentUser.id(jwt)));
	}

	@PostMapping("/notifications/{notificationId}/read")
	NotificationResponse markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID notificationId) {
		return this.notificationService.markRead(CurrentUser.id(jwt), notificationId);
	}

	@PostMapping("/notifications/read-all")
	Map<String, Integer> markAllRead(@AuthenticationPrincipal Jwt jwt) {
		return Map.of("updated", this.notificationService.markAllRead(CurrentUser.id(jwt)));
	}

	/** Called by the app on start and whenever FCM rotates the token. */
	@PostMapping("/me/devices")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void registerDevice(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeviceRequest request) {
		this.notificationService.registerDevice(CurrentUser.id(jwt), request.token(), request.platform());
	}

	/** Called by the app on logout so the device stops receiving this user's notifications. */
	@DeleteMapping("/me/devices/{token}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void unregisterDevice(@AuthenticationPrincipal Jwt jwt, @PathVariable String token) {
		this.notificationService.unregisterDevice(CurrentUser.id(jwt), token);
	}

}
