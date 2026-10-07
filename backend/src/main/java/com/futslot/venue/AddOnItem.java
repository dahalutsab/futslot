package com.futslot.venue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Something a venue sells or rents with a booking: bibs, a ball, water. Retired rather than deleted. */
@Entity
@Table(name = "add_on_items")
public class AddOnItem {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false, updatable = false)
	private UUID venueId;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AddOnCategory category;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(nullable = false)
	private boolean active = true;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected AddOnItem() {
	}

	AddOnItem(UUID venueId, AddOnRequest request) {
		this.venueId = venueId;
		apply(request);
	}

	void apply(AddOnRequest request) {
		this.name = request.name().strip();
		this.category = request.category();
		this.price = request.price();
		if (request.active() != null) {
			this.active = request.active();
		}
	}

	public UUID getId() {
		return this.id;
	}

	public UUID getVenueId() {
		return this.venueId;
	}

	public String getName() {
		return this.name;
	}

	public AddOnCategory getCategory() {
		return this.category;
	}

	public BigDecimal getPrice() {
		return this.price;
	}

	public boolean isActive() {
		return this.active;
	}

}
