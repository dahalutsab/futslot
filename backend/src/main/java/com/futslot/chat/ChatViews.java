package com.futslot.chat;

import java.time.Instant;
import java.util.UUID;

/** API shapes for chat. Players see venue replies signed by the venue; the venue sees which staff wrote. */
public final class ChatViews {

	private ChatViews() {
	}

	public record ConversationSummary(UUID id, UUID venueId, String venueName, String venueSlug, UUID playerId,
			String playerName, String lastMessage, Instant lastMessageAt, long unread) {

	}

	/** {@code fromYou}: sent by the viewer's side (the player, or anyone at the venue). */
	public record MessageView(UUID id, String senderName, boolean fromYou, UUID bookingId, String body, Instant readAt,
			Instant createdAt) {

	}

}
