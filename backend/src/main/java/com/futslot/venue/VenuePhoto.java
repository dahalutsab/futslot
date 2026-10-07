package com.futslot.venue;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A picture of the venue. One per venue is the cover shown in listings. */
@Entity
@Table(name = "venue_photos")
public class VenuePhoto {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false, updatable = false)
	private UUID venueId;

	@Column(nullable = false, updatable = false)
	private String url;

	private String caption;

	@Column(nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private boolean isCover;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected VenuePhoto() {
	}

	VenuePhoto(UUID venueId, String url, String caption, int sortOrder, boolean cover) {
		this.venueId = venueId;
		this.url = url;
		this.caption = caption;
		this.sortOrder = sortOrder;
		this.isCover = cover;
	}

	void setCaption(String caption) {
		this.caption = caption;
	}

	void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	void setCover(boolean cover) {
		this.isCover = cover;
	}

	public UUID getId() {
		return this.id;
	}

	public UUID getVenueId() {
		return this.venueId;
	}

	public String getUrl() {
		return this.url;
	}

	public String getCaption() {
		return this.caption;
	}

	public int getSortOrder() {
		return this.sortOrder;
	}

	public boolean isCover() {
		return this.isCover;
	}

}
