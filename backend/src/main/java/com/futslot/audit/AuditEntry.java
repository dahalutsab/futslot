package com.futslot.audit;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import com.futslot.common.Timestamps;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** One recorded action. Written in the same transaction as the change, so they stand or fall together. */
@Entity
@Table(name = "audit_logs")
class AuditEntry {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(updatable = false)
	private UUID actorId;

	@Column(updatable = false)
	private UUID venueId;

	@Column(nullable = false, updatable = false)
	private String action;

	@Column(nullable = false, updatable = false)
	private String entityType;

	@Column(updatable = false)
	private UUID entityId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(updatable = false)
	private Map<String, String> changes;

	@JdbcTypeCode(SqlTypes.INET)
	@Column(updatable = false)
	private InetAddress ipAddress;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected AuditEntry() {
	}

	AuditEntry(UUID actorId, UUID venueId, String action, String entityType, UUID entityId, Map<String, String> changes,
			InetAddress ipAddress) {
		this.actorId = actorId;
		this.venueId = venueId;
		this.action = action;
		this.entityType = entityType;
		this.entityId = entityId;
		this.changes = changes;
		this.ipAddress = ipAddress;
	}

	UUID getId() {
		return this.id;
	}

	UUID getActorId() {
		return this.actorId;
	}

	UUID getVenueId() {
		return this.venueId;
	}

	String getAction() {
		return this.action;
	}

	String getEntityType() {
		return this.entityType;
	}

	UUID getEntityId() {
		return this.entityId;
	}

	Map<String, String> getChanges() {
		return this.changes;
	}

	InetAddress getIpAddress() {
		return this.ipAddress;
	}

	Instant getCreatedAt() {
		return this.createdAt;
	}

	@PrePersist
	void stampCreated() {
		if (this.createdAt == null) {
			this.createdAt = Timestamps.now();
		}
	}

}
