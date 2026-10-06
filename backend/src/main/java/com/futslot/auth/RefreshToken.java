package com.futslot.auth;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.futslot.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Only the SHA-256 of the token is stored; the raw value exists only on the client. */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(nullable = false, unique = true)
	private String tokenHash;

	private String deviceInfo;

	@Column(nullable = false)
	private Instant expiresAt;

	private Instant revokedAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected RefreshToken() {
	}

	RefreshToken(User user, String tokenHash, Instant createdAt, Instant expiresAt) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	boolean isRevoked() {
		return this.revokedAt != null;
	}

	boolean isExpired(Instant now) {
		return !now.isBefore(this.expiresAt);
	}

	void revoke(Instant at) {
		if (this.revokedAt == null) {
			this.revokedAt = at;
		}
	}

	User getUser() {
		return this.user;
	}

}
