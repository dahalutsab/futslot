package com.futslot.review;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.futslot.booking.Booking;
import com.futslot.common.Timestamps;
import com.futslot.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** A player's rating of a venue, one per completed booking. */
@Entity
@Table(name = "reviews")
public class Review {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false, updatable = false)
	private UUID venueId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", updatable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "booking_id", updatable = false)
	private Booking booking;

	@Column(nullable = false)
	private short rating;

	private String comment;

	private String ownerReply;

	private Instant ownerRepliedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReviewStatus status = ReviewStatus.PUBLISHED;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected Review() {
	}

	Review(Booking booking, User user, int rating, String comment) {
		this.venueId = booking.getVenue().getId();
		this.booking = booking;
		this.user = user;
		edit(rating, comment);
	}

	void edit(int rating, String comment) {
		this.rating = (short) rating;
		this.comment = comment;
	}

	void reply(String text, Instant at) {
		this.ownerReply = text;
		this.ownerRepliedAt = (text != null) ? at : null;
	}

	void moderate(ReviewStatus status) {
		this.status = status;
	}

	public UUID getId() {
		return this.id;
	}

	public UUID getVenueId() {
		return this.venueId;
	}

	public User getUser() {
		return this.user;
	}

	public Booking getBooking() {
		return this.booking;
	}

	public int getRating() {
		return this.rating;
	}

	public String getComment() {
		return this.comment;
	}

	public String getOwnerReply() {
		return this.ownerReply;
	}

	public Instant getOwnerRepliedAt() {
		return this.ownerRepliedAt;
	}

	public ReviewStatus getStatus() {
		return this.status;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public Instant getUpdatedAt() {
		return this.updatedAt;
	}

	@PrePersist
	void stampCreated() {
		if (this.createdAt == null) {
			this.createdAt = Timestamps.now();
		}
	}

}
