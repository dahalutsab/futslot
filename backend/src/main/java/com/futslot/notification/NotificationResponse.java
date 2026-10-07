package com.futslot.notification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(UUID id, NotificationType type, String title, String body,
		Map<String, String> data, boolean read, Instant createdAt) {

	static NotificationResponse from(Notification notification) {
		return new NotificationResponse(notification.getId(), notification.getType(), notification.getTitle(),
				notification.getBody(), notification.getData(), notification.getReadAt() != null,
				notification.getCreatedAt());
	}

}
