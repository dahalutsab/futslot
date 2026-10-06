package com.futslot.auth;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "otp_codes")
public class OtpCode {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false)
	private String phone;

	@Column(nullable = false)
	private String codeHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OtpPurpose purpose;

	@Column(nullable = false)
	private int attempts;

	@Column(nullable = false)
	private Instant expiresAt;

	private Instant consumedAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected OtpCode() {
	}

	OtpCode(String phone, String codeHash, OtpPurpose purpose, Instant createdAt, Instant expiresAt) {
		this.phone = phone;
		this.codeHash = codeHash;
		this.purpose = purpose;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	boolean isUsable(Instant now, int maxAttempts) {
		return this.consumedAt == null && now.isBefore(this.expiresAt) && this.attempts < maxAttempts;
	}

	void recordFailedAttempt() {
		this.attempts++;
	}

	void consume(Instant at) {
		this.consumedAt = at;
	}

	String getCodeHash() {
		return this.codeHash;
	}

	Instant getCreatedAt() {
		return this.createdAt;
	}

}
