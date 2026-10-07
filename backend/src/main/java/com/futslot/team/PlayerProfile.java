package com.futslot.team;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import com.futslot.common.Timestamps;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * What a player shares about their game. Created on first save. The stored games_played column is not
 * used: games are counted from completed bookings and joined matches when shown.
 */
@Entity
@Table(name = "player_profiles")
public class PlayerProfile {

	@Id
	private UUID userId;

	@Enumerated(EnumType.STRING)
	private Position position;

	@Enumerated(EnumType.STRING)
	private SkillLevel skillLevel;

	@Enumerated(EnumType.STRING)
	private PreferredFoot preferredFoot;

	private String city;

	private String bio;

	@Column(nullable = false)
	private boolean isPublic = true;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected PlayerProfile() {
	}

	PlayerProfile(UUID userId) {
		this.userId = userId;
	}

	void apply(PlayerProfileRequest request) {
		this.position = request.position();
		this.skillLevel = request.skillLevel();
		this.preferredFoot = request.preferredFoot();
		this.city = blankToNull(request.city());
		this.bio = blankToNull(request.bio());
		if (request.isPublic() != null) {
			this.isPublic = request.isPublic();
		}
	}

	private static String blankToNull(String value) {
		return (value != null && !value.isBlank()) ? value.strip() : null;
	}

	public UUID getUserId() {
		return this.userId;
	}

	public Position getPosition() {
		return this.position;
	}

	public SkillLevel getSkillLevel() {
		return this.skillLevel;
	}

	public PreferredFoot getPreferredFoot() {
		return this.preferredFoot;
	}

	public String getCity() {
		return this.city;
	}

	public String getBio() {
		return this.bio;
	}

	public boolean isPublic() {
		return this.isPublic;
	}

	@PrePersist
	void stampCreated() {
		if (this.createdAt == null) {
			this.createdAt = Timestamps.now();
		}
	}

}
