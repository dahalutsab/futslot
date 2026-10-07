package com.futslot.chat;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.futslot.common.Timestamps;
import com.futslot.user.User;
import com.futslot.venue.Venue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** The one thread between a player and a venue. */
@Entity
@Table(name = "conversations")
class Conversation {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "venue_id", updatable = false)
	private Venue venue;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "player_id", updatable = false)
	private User player;

	private Instant lastMessageAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected Conversation() {
	}

	Conversation(Venue venue, User player) {
		this.venue = venue;
		this.player = player;
	}

	void touched(Instant at) {
		this.lastMessageAt = at;
	}

	UUID getId() {
		return this.id;
	}

	Venue getVenue() {
		return this.venue;
	}

	User getPlayer() {
		return this.player;
	}

	Instant getLastMessageAt() {
		return this.lastMessageAt;
	}

	@PrePersist
	void stampCreated() {
		if (this.createdAt == null) {
			this.createdAt = Timestamps.now();
		}
	}

}
