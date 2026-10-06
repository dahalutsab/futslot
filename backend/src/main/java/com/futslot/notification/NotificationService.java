package com.futslot.notification;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.ApiException;
import com.futslot.common.PageResponse;

@Service
public class NotificationService {

	private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

	private final NotificationRepository notifications;

	private final DeviceTokenRepository devices;

	private final PushSender pushSender;

	private final Clock clock;

	NotificationService(NotificationRepository notifications, DeviceTokenRepository devices, PushSender pushSender,
			Clock clock) {
		this.notifications = notifications;
		this.devices = devices;
		this.pushSender = pushSender;
		this.clock = clock;
	}

	/** Stores the notification in the user's inbox and pushes it to their devices. Never throws on push failure. */
	@Transactional
	public void send(UUID userId, NotificationType type, String title, String body, Map<String, String> data) {
		Notification notification = this.notifications.save(new Notification(userId, type, title, body, data));
		List<String> tokens = this.devices.findByUserId(userId).stream().map(DeviceToken::getToken).toList();
		if (tokens.isEmpty()) {
			notification.delivered(Channel.IN_APP, true, this.clock.instant());
			return;
		}
		boolean delivered;
		try {
			PushSender.Result result = this.pushSender.send(tokens, title, body, data);
			if (!result.invalidTokens().isEmpty()) {
				this.devices.deleteByTokenIn(result.invalidTokens());
			}
			delivered = result.delivered();
		}
		catch (RuntimeException ex) {
			logger.warn("Push for notification {} failed", notification.getId(), ex);
			delivered = false;
		}
		notification.delivered(Channel.PUSH, delivered, this.clock.instant());
	}

	@Transactional(readOnly = true)
	public PageResponse<NotificationResponse> inbox(UUID userId, int page, int size) {
		return PageResponse.of(this.notifications.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size)),
				NotificationResponse::from);
	}

	@Transactional(readOnly = true)
	public long unreadCount(UUID userId) {
		return this.notifications.countByUserIdAndReadAtIsNull(userId);
	}

	@Transactional
	public NotificationResponse markRead(UUID userId, UUID notificationId) {
		Notification notification = this.notifications.findByIdAndUserId(notificationId, userId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Notification not found"));
		notification.markRead(this.clock.instant());
		return NotificationResponse.from(notification);
	}

	@Transactional
	public int markAllRead(UUID userId) {
		return this.notifications.markAllRead(userId, this.clock.instant());
	}

	@Transactional
	public void registerDevice(UUID userId, String token, Platform platform) {
		this.devices.findByToken(token)
			.ifPresentOrElse(device -> device.claim(userId, platform, this.clock.instant()),
					() -> this.devices.save(new DeviceToken(userId, token, platform, this.clock.instant())));
	}

	@Transactional
	public void unregisterDevice(UUID userId, String token) {
		this.devices.deleteByTokenAndUserId(token, userId);
	}

}
