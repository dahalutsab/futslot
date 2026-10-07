package com.futslot.site;

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

/** A venue's website: its template, address and look. */
@Entity
@Table(name = "venue_sites")
class VenueSite {

	@Id
	private UUID venueId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SiteTemplate template;

	@Column(nullable = false)
	private String subdomain;

	private String customDomain;

	private String accentColor;

	private String headline;

	@Column(nullable = false)
	private boolean published;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected VenueSite() {
	}

	VenueSite(UUID venueId, SiteTemplate template, String subdomain) {
		this.venueId = venueId;
		this.template = template;
		this.subdomain = subdomain;
	}

	void update(SiteTemplate template, String subdomain, String customDomain, String accentColor, String headline) {
		this.template = template;
		this.subdomain = subdomain;
		this.customDomain = customDomain;
		this.accentColor = accentColor;
		this.headline = headline;
	}

	void setPublished(boolean published) {
		this.published = published;
	}

	UUID getVenueId() {
		return this.venueId;
	}

	SiteTemplate getTemplate() {
		return this.template;
	}

	String getSubdomain() {
		return this.subdomain;
	}

	String getCustomDomain() {
		return this.customDomain;
	}

	String getAccentColor() {
		return this.accentColor;
	}

	String getHeadline() {
		return this.headline;
	}

	boolean isPublished() {
		return this.published;
	}

	Instant getUpdatedAt() {
		return this.updatedAt;
	}

	@PrePersist
	void stampCreated() {
		if (this.createdAt == null) {
			this.createdAt = Timestamps.now();
		}
		if (this.updatedAt == null) {
			this.updatedAt = this.createdAt;
		}
	}

}
