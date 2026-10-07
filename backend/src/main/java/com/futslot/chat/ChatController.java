package com.futslot.chat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.futslot.common.CurrentUser;

import jakarta.validation.Valid;

/** Message lists are newest first, 50 at a time; pass the oldest {@code createdAt} as {@code before} for more. */
@RestController
@RequestMapping("/api/v1")
class ChatController {

	private final ChatService chatService;

	ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	// Players

	@PostMapping("/venues/{venueId}/messages")
	@ResponseStatus(HttpStatus.CREATED)
	ChatViews.MessageView messageVenue(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@Valid @RequestBody MessageRequest request) {
		return this.chatService.messageVenue(CurrentUser.id(jwt), venueId, request);
	}

	@GetMapping("/me/conversations")
	List<ChatViews.ConversationSummary> mine(@AuthenticationPrincipal Jwt jwt) {
		return this.chatService.forPlayer(CurrentUser.id(jwt));
	}

	@GetMapping("/me/conversations/{conversationId}/messages")
	List<ChatViews.MessageView> myMessages(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID conversationId,
			@RequestParam(required = false) Instant before) {
		return this.chatService.playerMessages(CurrentUser.id(jwt), conversationId, before);
	}

	@PostMapping("/me/conversations/{conversationId}/messages")
	@ResponseStatus(HttpStatus.CREATED)
	ChatViews.MessageView reply(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID conversationId,
			@Valid @RequestBody MessageRequest request) {
		return this.chatService.replyAsPlayer(CurrentUser.id(jwt), conversationId, request);
	}

	@PostMapping("/me/conversations/{conversationId}/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void read(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID conversationId) {
		this.chatService.playerRead(CurrentUser.id(jwt), conversationId);
	}

	// Venue

	@GetMapping("/owner/venues/{venueId}/conversations")
	List<ChatViews.ConversationSummary> forVenue(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId) {
		return this.chatService.forVenue(CurrentUser.id(jwt), venueId);
	}

	@GetMapping("/owner/venues/{venueId}/conversations/{conversationId}/messages")
	List<ChatViews.MessageView> venueMessages(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID conversationId, @RequestParam(required = false) Instant before) {
		return this.chatService.venueMessages(CurrentUser.id(jwt), venueId, conversationId, before);
	}

	@PostMapping("/owner/venues/{venueId}/conversations/{conversationId}/messages")
	@ResponseStatus(HttpStatus.CREATED)
	ChatViews.MessageView venueReply(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId,
			@PathVariable UUID conversationId, @Valid @RequestBody MessageRequest request) {
		return this.chatService.replyAsVenue(CurrentUser.id(jwt), venueId, conversationId, request);
	}

	@PostMapping("/owner/venues/{venueId}/conversations/{conversationId}/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void venueRead(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID venueId, @PathVariable UUID conversationId) {
		this.chatService.venueRead(CurrentUser.id(jwt), venueId, conversationId);
	}

}
