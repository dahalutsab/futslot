package com.futslot.chat;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.futslot.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One message. The sender is the player, or whoever at the venue replied. */
@Entity
@Table(name = "messages")
class Message {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "conversation_id", updatable = false)
	private Conversation conversation;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sender_id", updatable = false)
	private User sender;

	@Column(updatable = false)
	private UUID bookingId;

	@Column(nullable = false, updatable = false)
	private String body;

	private Instant readAt;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected Message() {
	}

	Message(Conversation conversation, User sender, UUID bookingId, String body) {
		this.conversation = conversation;
		this.sender = sender;
		this.bookingId = bookingId;
		this.body = body;
	}

	boolean fromPlayer() {
		return this.sender.getId().equals(this.conversation.getPlayer().getId());
	}

	UUID getId() {
		return this.id;
	}

	Conversation getConversation() {
		return this.conversation;
	}

	User getSender() {
		return this.sender;
	}

	UUID getBookingId() {
		return this.bookingId;
	}

	String getBody() {
		return this.body;
	}

	Instant getReadAt() {
		return this.readAt;
	}

	Instant getCreatedAt() {
		return this.createdAt;
	}

}
