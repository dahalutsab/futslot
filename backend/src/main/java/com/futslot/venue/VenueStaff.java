package com.futslot.venue;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
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
@Table(name = "venue_staff")
public class VenueStaff {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false, updatable = false)
	private UUID venueId;

	@Column(nullable = false, updatable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StaffRole role;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(nullable = false, columnDefinition = "text[]")
	private String[] permissions = new String[0];

	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false, updatable = false)
	private UUID invitedBy;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected VenueStaff() {
	}

	VenueStaff(UUID venueId, UUID userId, UUID invitedBy, StaffRole role, Set<VenuePermission> permissions) {
		this.venueId = venueId;
		this.userId = userId;
		this.invitedBy = invitedBy;
		assign(role, permissions, true);
	}

	void assign(StaffRole role, Set<VenuePermission> permissions, boolean active) {
		this.role = role;
		this.permissions = permissions.stream().map(Enum::name).sorted().toArray(String[]::new);
		this.active = active;
	}

	public boolean has(VenuePermission permission) {
		return this.active && getPermissions().contains(permission);
	}

	public UUID getId() {
		return this.id;
	}

	public UUID getVenueId() {
		return this.venueId;
	}

	public UUID getUserId() {
		return this.userId;
	}

	public StaffRole getRole() {
		return this.role;
	}

	public Set<VenuePermission> getPermissions() {
		Set<VenuePermission> result = EnumSet.noneOf(VenuePermission.class);
		Arrays.stream(this.permissions).map(VenuePermission::valueOf).forEach(result::add);
		return result;
	}

	public boolean isActive() {
		return this.active;
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
