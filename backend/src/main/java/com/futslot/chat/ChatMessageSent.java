package com.futslot.chat;

import java.util.List;
import java.util.UUID;

/** For notifications: who should hear about a new message. */
public record ChatMessageSent(UUID conversationId, List<UUID> recipients, String fromName, String preview,
		boolean toVenue, UUID venueId) {

}
