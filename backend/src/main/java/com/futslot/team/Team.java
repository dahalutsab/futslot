package com.futslot.team;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

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
@Table(name = "teams")
public class Team {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true, updatable = false)
	private String slug;

	private String logoUrl;

	@Column(nullable = false)
	private UUID captainId;

	private String city;

	@Enumerated(EnumType.STRING)
	private SkillLevel skillLevel;

	@Column(nullable = false)
	private boolean isPublic = true;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected Team() {
	}

	Team(String slug, UUID captainId, TeamRequest request) {
		this.slug = slug;
		this.captainId = captainId;
		apply(request);
	}

	void apply(TeamRequest request) {
		this.name = request.name().strip();
		this.city = (request.city() != null && !request.city().isBlank()) ? request.city().strip() : null;
		this.skillLevel = request.skillLevel();
		this.logoUrl = (request.logoUrl() != null && !request.logoUrl().isBlank()) ? request.logoUrl().strip() : null;
		if (request.isPublic() != null) {
			this.isPublic = request.isPublic();
		}
	}

	void setCaptain(UUID userId) {
		this.captainId = userId;
	}

	public UUID getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public String getSlug() {
		return this.slug;
	}

	public String getLogoUrl() {
		return this.logoUrl;
	}

	public UUID getCaptainId() {
		return this.captainId;
	}

	public String getCity() {
		return this.city;
	}

	public SkillLevel getSkillLevel() {
		return this.skillLevel;
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
