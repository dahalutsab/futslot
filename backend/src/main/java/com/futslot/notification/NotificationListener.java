package com.futslot.notification;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import com.futslot.booking.BookingEvent;
import com.futslot.booking.BookingSource;
import com.futslot.booking.BookingStatus;
import com.futslot.booking.RecurringBookingEvent;
import com.futslot.matchmaking.MatchEvent;
import com.futslot.matchmaking.MatchPostType;
import com.futslot.payment.MembershipPaymentEvent;
import com.futslot.payment.PaymentEvent;
import com.futslot.review.ReviewEvent;
import com.futslot.split.SplitEvent;
import com.futslot.team.TeamEvent;
import com.futslot.team.TeamService;
import com.futslot.venue.StaffAdded;
import com.futslot.venue.VenueAccess;
import com.futslot.venue.VenueModerated;
import com.futslot.venue.VenuePermission;

/**
 * Turns domain events into notifications once the change has committed. Runs in its own transaction
 * because the originating one is already finished at that point.
 */
@Component
class NotificationListener {

	private final NotificationService notifications;

	private final VenueAccess access;

	private final TeamService teams;

	NotificationListener(NotificationService notifications, VenueAccess access, TeamService teams) {
		this.notifications = notifications;
		this.access = access;
		this.teams = teams;
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(BookingEvent event) {
		String slot = event.courtName() + ", " + Messages.when(event.startsAt());
		Map<String, String> data = Map.of("bookingId", event.bookingId().toString(), "venueId", event.venueId().toString());
		switch (event.type()) {
			case CREATED -> created(event, slot, data);
			case CANCELLED -> cancelled(event, slot, data);
			case RESCHEDULED -> toPlayer(event, NotificationType.BOOKING_RESCHEDULED, "Your booking was moved",
					event.venueName() + " moved your booking to " + slot + ".", data);
			case EXPIRED -> toPlayer(event, NotificationType.BOOKING_EXPIRED, "Booking hold expired",
					"Payment wasn't completed in time, so your hold on " + event.venueName() + ", " + slot
							+ " was released.", data);
		}
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(VenueModerated event) {
		Map<String, String> data = Map.of("venueId", event.venueId().toString());
		switch (event.status()) {
			case ACTIVE -> this.notifications.send(event.ownerId(), NotificationType.VENUE_APPROVED, "Your venue is live",
					event.venueName() + " is approved and visible to players.", data);
			case REJECTED -> this.notifications.send(event.ownerId(), NotificationType.VENUE_REJECTED,
					"Venue not approved", event.venueName() + ": " + event.reason(), data);
			case SUSPENDED -> this.notifications.send(event.ownerId(), NotificationType.VENUE_SUSPENDED,
					"Venue suspended", event.venueName() + " is hidden from players: " + event.reason(), data);
			default -> {
			}
		}
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(RecurringBookingEvent event) {
		String weekly = Messages.weekly(event.dayOfWeek(), event.startTime(), event.endTime());
		Map<String, String> data = Map.of("recurringBookingId", event.seriesId().toString(), "venueId",
				event.venueId().toString());
		String skipped = event.skippedDates().stream().map(Messages::day).collect(Collectors.joining(", "));
		switch (event.type()) {
			case CREATED -> {
				if (event.playerUserId() != null && !event.playerUserId().equals(event.actorUserId())) {
					this.notifications.send(event.playerUserId(), NotificationType.RECURRING_BOOKING_CREATED,
							"Weekly booking at " + event.venueName(),
							weekly + " on " + event.courtName() + ", from " + Messages.date(event.startsOn()) + "."
									+ (skipped.isEmpty() ? "" : " Not available: " + skipped + "."),
							data);
				}
			}
			case CANCELLED -> {
				if (event.playerUserId() != null && !event.playerUserId().equals(event.actorUserId())) {
					this.notifications.send(event.playerUserId(), NotificationType.RECURRING_BOOKING_CANCELLED,
							event.venueName() + " ended your weekly booking", weekly + " on " + event.courtName() + ".",
							data);
				}
			}
			case SKIPPED -> {
				String body = event.courtName() + " was taken or blocked on " + skipped + ", so " + weekly
						+ " wasn't booked those weeks.";
				this.access.usersWith(event.venueId(), VenuePermission.MANAGE_BOOKINGS)
					.forEach(userId -> this.notifications.send(userId, NotificationType.RECURRING_BOOKING_SKIPPED,
							"Weekly slot for " + event.customerName() + " not booked", body, data));
				if (event.playerUserId() != null) {
					this.notifications.send(event.playerUserId(), NotificationType.RECURRING_BOOKING_SKIPPED,
							"Weekly slot at " + event.venueName() + " not booked", body, data);
				}
			}
		}
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(PaymentEvent event) {
		String slot = Messages.when(event.startsAt());
		String amount = Messages.rupees(event.amount());
		Map<String, String> data = Map.of("bookingId", event.bookingId().toString(), "venueId", event.venueId().toString());
		switch (event.type()) {
			case SUBMITTED -> this.access.usersWith(event.venueId(), VenuePermission.MANAGE_BOOKINGS)
				.forEach(userId -> this.notifications.send(userId, NotificationType.PAYMENT_TO_VERIFY, "Payment to verify",
						event.customerName() + " paid " + amount + " by " + event.method() + " for " + slot
								+ ". Check your wallet, then approve or reject.",
						data));
			case RECEIVED -> {
				if (event.playerUserId() != null && !event.playerUserId().equals(event.actorUserId())) {
					String confirmed = (event.bookingStatus() == BookingStatus.CONFIRMED) ? " Your booking is confirmed." : "";
					this.notifications.send(event.playerUserId(), NotificationType.PAYMENT_RECEIVED, "Payment received",
							event.venueName() + " received " + amount + " for " + slot + "." + confirmed, data);
				}
			}
			case REJECTED -> {
				if (event.playerUserId() != null) {
					String deadline = (event.holdExpiresAt() != null) ? " Pay by "
							+ Messages.time(event.holdExpiresAt().atZone(event.startsAt().getOffset()).toOffsetDateTime())
							+ " to keep the slot." : "";
					this.notifications.send(event.playerUserId(), NotificationType.PAYMENT_REJECTED, "Payment not found",
							event.venueName() + " couldn't find your " + amount + " payment: " + event.reason() + "."
									+ deadline,
							data);
				}
			}
			case REFUND_DUE -> {
				if (event.playerUserId() != null) {
					this.notifications.send(event.playerUserId(), NotificationType.REFUND_DUE, "Refund on the way",
							event.venueName() + " will refund " + amount + " for " + slot + ".", data);
				}
				this.access.usersWith(event.venueId(), VenuePermission.MANAGE_BOOKINGS)
					.forEach(userId -> this.notifications.send(userId, NotificationType.REFUND_TO_PAY, "Refund to pay",
							event.customerName() + " is owed " + amount + " for " + slot + " (" + event.reason() + ").",
							data));
			}
			case REFUNDED -> {
				if (event.playerUserId() != null) {
					this.notifications.send(event.playerUserId(), NotificationType.REFUND_SENT, "Refund sent",
							event.venueName() + " refunded " + amount + " for " + slot + ".", data);
				}
			}
		}
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(MembershipPaymentEvent event) {
		String amount = Messages.rupees(event.amount());
		Map<String, String> data = Map.of("membershipId", event.membershipId().toString(), "venueId",
				event.venueId().toString());
		switch (event.type()) {
			case SUBMITTED -> this.access.usersWith(event.venueId(), VenuePermission.MANAGE_BOOKINGS)
				.forEach(userId -> this.notifications.send(userId, NotificationType.PAYMENT_TO_VERIFY, "Payment to verify",
						event.customerName() + " paid " + amount + " by " + event.method() + " for " + event.planName()
								+ ". Check your wallet, then approve or reject.",
						data));
			case RECEIVED -> {
				if (event.playerUserId() != null && !event.playerUserId().equals(event.actorUserId())) {
					this.notifications.send(event.playerUserId(), NotificationType.MEMBERSHIP_ACTIVATED,
							"Membership active", event.planName() + " at " + event.venueName() + " is valid until "
									+ Messages.date(event.expiresOn()) + ".",
							data);
				}
			}
			case REJECTED -> {
				if (event.playerUserId() != null) {
					this.notifications.send(event.playerUserId(), NotificationType.PAYMENT_REJECTED, "Payment not found",
							event.venueName() + " couldn't find your " + amount + " payment for " + event.planName()
									+ ": " + event.reason() + ".",
							data);
				}
			}
			default -> {
			}
		}
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(ReviewEvent event) {
		Map<String, String> data = Map.of("reviewId", event.reviewId().toString(), "venueId", event.venueId().toString());
		switch (event.type()) {
			case CREATED -> {
				String comment = (event.comment() != null) ? ": \"" + Messages.excerpt(event.comment()) + "\"" : ".";
				this.access.usersWith(event.venueId(), VenuePermission.MANAGE_VENUE)
					.forEach(userId -> this.notifications.send(userId, NotificationType.NEW_REVIEW,
							"New " + event.rating() + "-star review",
							event.authorName() + " rated " + event.venueName() + " " + event.rating() + "/5" + comment,
							data));
			}
			case REPLIED -> this.notifications.send(event.authorUserId(), NotificationType.REVIEW_REPLY,
					event.venueName() + " replied to your review", "Tap to read their reply.", data);
		}
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(TeamEvent event) {
		Map<String, String> data = Map.of("teamId", event.teamId().toString(), "teamSlug", event.teamSlug());
		switch (event.type()) {
			case INVITED -> this.notifications.send(event.userId(), NotificationType.TEAM_INVITE,
					"Join " + event.teamName() + "?", event.actorName() + " invited you to the team.", data);
			case JOINED -> this.teams.squad(event.teamId())
				.stream()
				.filter(userId -> !userId.equals(event.userId()))
				.forEach(userId -> this.notifications.send(userId, NotificationType.TEAM_MEMBER_JOINED,
						event.userName() + " joined " + event.teamName(), "Say hello to your new teammate.", data));
			case REMOVED -> this.notifications.send(event.userId(), NotificationType.TEAM_MEMBER_REMOVED,
					"Removed from " + event.teamName(), event.actorName() + " removed you from the team.", data);
			case CAPTAIN_CHANGED -> this.notifications.send(event.userId(), NotificationType.TEAM_CAPTAIN_CHANGED,
					"You're captain of " + event.teamName(), event.actorName() + " handed you the captaincy.", data);
		}
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(MatchEvent event) {
		Map<String, String> data = Map.of("matchPostId", event.postId().toString());
		String match = event.place() + ", " + Messages.when(event.startsAt());
		String what = (event.postType() == MatchPostType.FIND_OPPONENT) ? "your match" : "your game";
		for (UUID recipient : event.recipients()) {
			switch (event.type()) {
				case REQUESTED -> this.notifications.send(recipient, NotificationType.MATCH_REQUEST,
						event.actorName() + " wants to join", match + ". Accept or decline in the app.", data);
				case ACCEPTED -> this.notifications.send(recipient, NotificationType.MATCH_REQUEST_ACCEPTED,
						"You're in!", event.actorName() + " accepted you for " + match + ".", data);
				case DECLINED -> this.notifications.send(recipient, NotificationType.MATCH_REQUEST_DECLINED,
						"Not this time", "Your request for " + match + " wasn't accepted.", data);
				case WITHDRAWN -> this.notifications.send(recipient, NotificationType.MATCH_PLAYER_WITHDREW,
						event.actorName() + " dropped out", "A place opened up in " + what + " at " + match + ".", data);
				case CANCELLED -> this.notifications.send(recipient, NotificationType.MATCH_POST_CANCELLED,
						"Match called off", event.actorName() + " cancelled " + match + ".", data);
			}
		}
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(SplitEvent event) {
		Map<String, String> data = Map.of("bookingId", event.bookingId().toString());
		String game = event.venueName() + ", " + Messages.when(event.startsAt());
		switch (event.type()) {
			case ADDED -> this.notifications.send(event.recipientUserId(), NotificationType.SPLIT_ADDED,
					event.bookerName() + " added you to a game", game + ". You're on " + event.sideName() + ".", data);
			case RESULT -> {
				String outcome = event.draw() ? "It's a draw" : event.winnerName() + " won";
				String owes = (event.share().signum() > 0)
						? "Your share is " + Messages.rupees(event.share()) + ", pay " + event.bookerName() + "."
						: "You pay nothing.";
				this.notifications.send(event.recipientUserId(), NotificationType.SPLIT_RESULT, outcome,
						game + ". " + owes, data);
			}
		}
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(StaffAdded event) {
		String role = event.role().name().charAt(0) + event.role().name().substring(1).toLowerCase();
		this.notifications.send(event.userId(), NotificationType.VENUE_STAFF_ADDED, "You've joined " + event.venueName(),
				"You were added as " + role + ". Open the venue dashboard to get started.",
				Map.of("venueId", event.venueId().toString()));
	}

	private void created(BookingEvent event, String slot, Map<String, String> data) {
		if (event.source() == BookingSource.APP) {
			if (event.status() == BookingStatus.PENDING_PAYMENT) {
				String deadline = Messages.time(event.holdExpiresAt().atZone(event.startsAt().getOffset()).toOffsetDateTime());
				toPlayer(event, NotificationType.BOOKING_PAYMENT_DUE, "Complete your payment",
						"Pay " + Messages.rupees(event.advanceRequired()) + " by " + deadline + " to confirm "
								+ event.venueName() + ", " + slot + ".", data);
			}
			else {
				toPlayer(event, NotificationType.BOOKING_CONFIRMED, "Booking confirmed",
						event.venueName() + ", " + slot + ". Ref " + event.reference() + ".", data);
			}
			toVenue(event, NotificationType.NEW_BOOKING, "New booking", event.customerName() + " booked " + slot + ".",
					data);
		}
		else {
			toPlayer(event, NotificationType.BOOKING_CREATED_BY_VENUE, event.venueName() + " booked a slot for you",
					slot + ". Ref " + event.reference() + ".", data);
		}
	}

	private void cancelled(BookingEvent event, String slot, Map<String, String> data) {
		String reason = (event.reason() != null && !event.reason().isBlank()) ? " Reason: " + event.reason() : "";
		if (Objects.equals(event.actorUserId(), event.playerUserId())) {
			toVenue(event, NotificationType.BOOKING_CANCELLED, "Booking cancelled",
					event.customerName() + " cancelled " + slot + "." + reason, data);
		}
		else {
			toPlayer(event, NotificationType.BOOKING_CANCELLED, event.venueName() + " cancelled your booking",
					slot + "." + reason, data);
		}
	}

	private void toPlayer(BookingEvent event, NotificationType type, String title, String body, Map<String, String> data) {
		if (event.playerUserId() != null) {
			this.notifications.send(event.playerUserId(), type, title, body, data);
		}
	}

	/** The owner and staff who handle bookings, except whoever made the change. */
	private void toVenue(BookingEvent event, NotificationType type, String title, String body, Map<String, String> data) {
		this.access.usersWith(event.venueId(), VenuePermission.MANAGE_BOOKINGS)
			.stream()
			.filter(userId -> !userId.equals(event.actorUserId()))
			.forEach(userId -> this.notifications.send(userId, type, title, body, data));
	}

}
