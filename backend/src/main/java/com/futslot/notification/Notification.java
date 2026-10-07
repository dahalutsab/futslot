package com.futslot.notification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import com.futslot.common.Timestamps;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications")
public class Notification {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false, updatable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private NotificationType type;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String body;

	/** Deep-link data for the app, e.g. {"bookingId": "..."}. */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private Map<String, String> data;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Channel channel = Channel.IN_APP;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DeliveryStatus status = DeliveryStatus.PENDING;

	private Instant sentAt;

	private Instant readAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected Notification() {
	}

	Notification(UUID userId, NotificationType type, String title, String body, Map<String, String> data) {
		this.userId = userId;
		this.type = type;
		this.title = title;
		this.body = body;
		this.data = data;
	}

	void delivered(Channel channel, boolean success, Instant at) {
		this.channel = channel;
		this.status = success ? DeliveryStatus.SENT : DeliveryStatus.FAILED;
		this.sentAt = success ? at : null;
	}

	void markRead(Instant at) {
		if (this.readAt == null) {
			this.readAt = at;
		}
	}

	public UUID getId() {
		return this.id;
	}

	public NotificationType getType() {
		return this.type;
	}

	public String getTitle() {
		return this.title;
	}

	public String getBody() {
		return this.body;
	}

	public Map<String, String> getData() {
		return this.data;
	}

	public Instant getReadAt() {
		return this.readAt;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	@PrePersist
	void stampCreated() {
		if (this.createdAt == null) {
			this.createdAt = Timestamps.now();
		}
	}

}
