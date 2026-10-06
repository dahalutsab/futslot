package com.futslot.booking;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A venue's own record of a customer, keyed by phone. Walk-ins have no app account (userId null). */
@Entity
@Table(name = "venue_customers")
public class VenueCustomer {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false, updatable = false)
	private UUID venueId;

	private UUID userId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, updatable = false)
	private String phone;

	private String notes;

	@Column(nullable = false)
	private int totalBookings;

	private Instant lastBookingAt;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected VenueCustomer() {
	}

	VenueCustomer(UUID venueId, String phone, String name) {
		this.venueId = venueId;
		this.phone = phone;
		this.name = name;
	}

	void linkUser(UUID userId) {
		if (this.userId == null) {
			this.userId = userId;
		}
	}

	void recordBooking(Instant at) {
		this.totalBookings++;
		this.lastBookingAt = at;
	}

	public UUID getId() {
		return this.id;
	}

	public UUID getUserId() {
		return this.userId;
	}

	public String getName() {
		return this.name;
	}

	public String getPhone() {
		return this.phone;
	}

	public int getTotalBookings() {
		return this.totalBookings;
	}

}
