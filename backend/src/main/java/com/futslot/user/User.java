package com.futslot.user;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.futslot.common.Timestamps;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false)
	private String fullName;

	@Column(nullable = false, unique = true)
	private String phone;

	@Column(columnDefinition = "citext")
	private String email;

	private String avatarUrl;

	private String passwordHash;

	private Instant passwordChangedAt;

	private int failedSignIns;

	private Instant signInLockedUntil;

	private String googleSubject;

	@Column(nullable = false)
	private String preferredLanguage = "en";

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserStatus status = UserStatus.ACTIVE;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false)
	private Set<Role> roles = EnumSet.noneOf(Role.class);

	private Instant phoneVerifiedAt;

	private Instant lastLoginAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected User() {
	}

	public User(String phone, String fullName) {
		this.phone = phone;
		this.fullName = fullName;
		this.roles.add(Role.PLAYER);
	}

	public boolean isActive() {
		return this.status == UserStatus.ACTIVE;
	}

	public void markPhoneVerified(Instant at) {
		if (this.phoneVerifiedAt == null) {
			this.phoneVerifiedAt = at;
		}
	}

	public void recordLogin(Instant at) {
		this.lastLoginAt = at;
		this.failedSignIns = 0;
		this.signInLockedUntil = null;
	}

	public boolean hasPassword() {
		return this.passwordHash != null;
	}

	public String getPasswordHash() {
		return this.passwordHash;
	}

	/** A new password also lifts any lock from wrong guesses at the old one. */
	public void changePassword(String passwordHash, Instant at) {
		this.passwordHash = passwordHash;
		this.passwordChangedAt = at;
		this.failedSignIns = 0;
		this.signInLockedUntil = null;
	}

	public boolean isSignInLocked(Instant now) {
		return this.signInLockedUntil != null && this.signInLockedUntil.isAfter(now);
	}

	/** Counts a wrong password; the {@code maxFailures}th in a row locks signing in until {@code lockedUntil}. */
	public void recordFailedSignIn(int maxFailures, Instant lockedUntil) {
		this.failedSignIns++;
		if (this.failedSignIns >= maxFailures) {
			this.failedSignIns = 0;
			this.signInLockedUntil = lockedUntil;
		}
	}

	public String getGoogleSubject() {
		return this.googleSubject;
	}

	public void linkGoogle(String subject) {
		this.googleSubject = subject;
	}

	public UUID getId() {
		return this.id;
	}

	public String getFullName() {
		return this.fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getPhone() {
		return this.phone;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAvatarUrl() {
		return this.avatarUrl;
	}

	public String getPreferredLanguage() {
		return this.preferredLanguage;
	}

	public void setPreferredLanguage(String preferredLanguage) {
		this.preferredLanguage = preferredLanguage;
	}

	public UserStatus getStatus() {
		return this.status;
	}

	public Set<Role> getRoles() {
		return this.roles;
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
