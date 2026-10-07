package com.futslot.notification;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A push token (FCM) for one app installation. A token belongs to whoever registered it last. */
@Entity
@Table(name = "device_tokens")
public class DeviceToken {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false)
	private UUID userId;

	@Column(nullable = false, unique = true, updatable = false)
	private String token;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Platform platform;

	@Column(nullable = false)
	private Instant lastSeenAt;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected DeviceToken() {
	}

	DeviceToken(UUID userId, String token, Platform platform, Instant at) {
		this.userId = userId;
		this.token = token;
		this.platform = platform;
		this.lastSeenAt = at;
	}

	void claim(UUID userId, Platform platform, Instant at) {
		this.userId = userId;
		this.platform = platform;
		this.lastSeenAt = at;
	}

	public String getToken() {
		return this.token;
	}

}
