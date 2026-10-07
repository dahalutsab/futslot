package com.futslot.chat;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.booking.Booking;
import com.futslot.booking.BookingLookup;
import com.futslot.common.ApiException;
import com.futslot.common.Names;
import com.futslot.user.User;
import com.futslot.user.UserRepository;
import com.futslot.venue.Venue;
import com.futslot.venue.VenueAccess;
import com.futslot.venue.VenuePermission;
import com.futslot.venue.VenueRepository;
import com.futslot.venue.VenueStatus;

/**
 * One thread per player and venue. Anyone at the venue who handles bookings can read and reply; the player
 * sees replies from the venue. New messages are pushed to the other side; there's no live socket, so the
 * apps refresh when a notification arrives.
 */
@Service
class ChatService {

	static final int PAGE = 50;

	private final ConversationRepository conversations;

	private final MessageRepository messages;

	private final VenueRepository venues;

	private final UserRepository users;

	private final VenueAccess access;

	private final BookingLookup bookings;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	ChatService(ConversationRepository conversations, MessageRepository messages, VenueRepository venues,
			UserRepository users, VenueAccess access, BookingLookup bookings, ApplicationEventPublisher events,
			Clock clock) {
		this.conversations = conversations;
		this.messages = messages;
		this.venues = venues;
		this.users = users;
		this.access = access;
		this.bookings = bookings;
		this.events = events;
		this.clock = clock;
	}

	// Players

	/** Messages a venue, starting the conversation if this is the first. */
	@Transactional
	ChatViews.MessageView messageVenue(UUID userId, UUID venueId, MessageRequest request) {
		Venue venue = this.venues.findById(venueId)
			.filter(found -> found.getStatus() == VenueStatus.ACTIVE)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VENUE_NOT_FOUND", "Venue not found"));
		User player = this.users.findById(userId).orElseThrow();
		Conversation conversation = this.conversations.findByVenueIdAndPlayerId(venueId, userId)
			.orElseGet(() -> this.conversations.save(new Conversation(venue, player)));
		return send(conversation, player, request, false);
	}

	@Transactional
	ChatViews.MessageView replyAsPlayer(UUID userId, UUID conversationId, MessageRequest request) {
		Conversation conversation = playerConversation(userId, conversationId);
		return send(conversation, conversation.getPlayer(), request, false);
	}

	@Transactional(readOnly = true)
	List<ChatViews.ConversationSummary> forPlayer(UUID userId) {
		return this.conversations.findForPlayer(userId).stream().map(c -> summary(c, false)).toList();
	}

	@Transactional(readOnly = true)
	List<ChatViews.MessageView> playerMessages(UUID userId, UUID conversationId, Instant before) {
		return page(playerConversation(userId, conversationId), before, false);
	}

	/** Marks the venue's messages as read. */
	@Transactional
	void playerRead(UUID userId, UUID conversationId) {
		Conversation conversation = playerConversation(userId, conversationId);
		this.messages.markRead(conversation.getId(), userId, false, this.clock.instant());
	}

	// Venue

	@Transactional
	ChatViews.MessageView replyAsVenue(UUID userId, UUID venueId, UUID conversationId, MessageRequest request) {
		Conversation conversation = venueConversation(userId, venueId, conversationId);
		return send(conversation, this.users.findById(userId).orElseThrow(), request, true);
	}

	@Transactional(readOnly = true)
	List<ChatViews.ConversationSummary> forVenue(UUID userId, UUID venueId) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_BOOKINGS);
		return this.conversations.findForVenue(venueId).stream().map(c -> summary(c, true)).toList();
	}

	@Transactional(readOnly = true)
	List<ChatViews.MessageView> venueMessages(UUID userId, UUID venueId, UUID conversationId, Instant before) {
		return page(venueConversation(userId, venueId, conversationId), before, true);
	}

	/** Marks the player's messages as read. */
	@Transactional
	void venueRead(UUID userId, UUID venueId, UUID conversationId) {
		Conversation conversation = venueConversation(userId, venueId, conversationId);
		this.messages.markRead(conversation.getId(), conversation.getPlayer().getId(), true, this.clock.instant());
	}

	private ChatViews.MessageView send(Conversation conversation, User sender, MessageRequest request, boolean fromVenue) {
		if (request.bookingId() != null) {
			// Only one of this player's bookings at this venue
			Booking booking = this.bookings.byId(request.bookingId());
			if (!booking.getVenue().getId().equals(conversation.getVenue().getId())
					|| !conversation.getPlayer().getId().equals(booking.getCustomer().getUserId())) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_BOOKING", "That booking isn't part of this conversation");
			}
		}
		Message message = this.messages.save(new Message(conversation, sender, request.bookingId(), request.body().strip()));
		conversation.touched(this.clock.instant());
		Venue venue = conversation.getVenue();
		List<UUID> recipients = fromVenue ? List.of(conversation.getPlayer().getId())
				: this.access.usersWith(venue.getId(), VenuePermission.MANAGE_BOOKINGS);
		String fromName = fromVenue ? venue.getName() : Names.shortName(conversation.getPlayer().getFullName());
		this.events.publishEvent(new ChatMessageSent(conversation.getId(), recipients.stream()
			.filter(id -> !id.equals(sender.getId()))
			.toList(), fromName, message.getBody(), !fromVenue, venue.getId()));
		return view(message, fromVenue);
	}

	private List<ChatViews.MessageView> page(Conversation conversation, Instant before, boolean venueSide) {
		Instant cursor = (before != null) ? before : this.clock.instant().plusSeconds(1);
		return this.messages.findPage(conversation.getId(), cursor, PageRequest.of(0, PAGE))
			.stream()
			.map(message -> view(message, venueSide))
			.toList();
	}

	private ChatViews.ConversationSummary summary(Conversation conversation, boolean venueSide) {
		String last = this.messages.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId())
			.map(Message::getBody)
			.orElse(null);
		Venue venue = conversation.getVenue();
		User player = conversation.getPlayer();
		return new ChatViews.ConversationSummary(conversation.getId(), venue.getId(), venue.getName(), venue.getSlug(),
				player.getId(), venueSide ? player.getFullName() : null, last, conversation.getLastMessageAt(),
				this.messages.countUnread(conversation.getId(), player.getId(), venueSide));
	}

	/** Players see venue messages as from the venue; the venue sees which of its people wrote. */
	private static ChatViews.MessageView view(Message message, boolean venueSide) {
		boolean fromPlayer = message.fromPlayer();
		String sender = fromPlayer ? message.getSender().getFullName()
				: venueSide ? message.getSender().getFullName() + " (" + message.getConversation().getVenue().getName() + ")"
						: message.getConversation().getVenue().getName();
		return new ChatViews.MessageView(message.getId(), sender, venueSide != fromPlayer, message.getBookingId(),
				message.getBody(), message.getReadAt(), message.getCreatedAt());
	}

	private Conversation playerConversation(UUID userId, UUID conversationId) {
		return this.conversations.findByIdAndPlayerId(conversationId, userId).orElseThrow(ChatService::notFound);
	}

	private Conversation venueConversation(UUID userId, UUID venueId, UUID conversationId) {
		this.access.require(userId, venueId, VenuePermission.MANAGE_BOOKINGS);
		return this.conversations.findByIdAndVenueId(conversationId, venueId).orElseThrow(ChatService::notFound);
	}

	private static ApiException notFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", "Conversation not found");
	}

}
