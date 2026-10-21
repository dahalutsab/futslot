package com.futslot.notification;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import com.futslot.billing.BillingEvent;
import com.futslot.booking.BookingEvent;
import com.futslot.chat.ChatMessageSent;
import com.futslot.common.Wording;
import com.futslot.common.Words;
import com.futslot.deposit.DepositEvent;
import com.futslot.deposit.DepositService;
import com.futslot.booking.BookingStatus;
import com.futslot.booking.RecurringBookingEvent;
import com.futslot.matchmaking.MatchEvent;
import com.futslot.matchmaking.MatchPostType;
import com.futslot.payment.EntryPaymentEvent;
import com.futslot.payment.MembershipPaymentEvent;
import com.futslot.payment.PaymentEvent;
import com.futslot.payment.PlatformPaymentRejected;
import com.futslot.review.ReviewEvent;
import com.futslot.split.SplitEvent;
import com.futslot.team.TeamEvent;
import com.futslot.team.TeamService;
import com.futslot.tournament.TournamentEvent;
import com.futslot.user.UserService;
import com.futslot.venue.StaffAdded;
import com.futslot.venue.VenueAccess;
import com.futslot.venue.VenueModerated;
import com.futslot.venue.VenuePermission;
import com.futslot.waitlist.SlotFreed;

/**
 * Turns domain events into notifications once the change has committed. Runs in its own transaction
 * because the originating one is already finished at that point. Players are told in their own language
 * (i18n/notices*.properties); venue owners, staff and admins in English, like the dashboard.
 */
@Component
class NotificationListener {

	private final NotificationService notifications;

	private final VenueAccess access;

	private final TeamService teams;

	private final UserService users;

	private final Words words;

	NotificationListener(NotificationService notifications, VenueAccess access, TeamService teams, UserService users,
			Words words) {
		this.notifications = notifications;
		this.access = access;
		this.teams = teams;
		this.users = users;
		this.words = words;
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(BookingEvent event) {
		String slot = event.courtName() + ", " + Messages.when(event.startsAt());
		Map<String, String> data = Map.of("bookingId", event.bookingId().toString(), "venueId", event.venueId().toString());
		switch (event.type()) {
			case CREATED -> created(event, slot, data);
			case CANCELLED -> cancelled(event, slot, data);
			case RESCHEDULED -> toPlayer(event, NotificationType.BOOKING_RESCHEDULED, data,
					w -> notice(w, "booking.moved", event.venueName(), slot(event, w)));
			case EXPIRED -> toPlayer(event, NotificationType.BOOKING_EXPIRED, data,
					w -> notice(w, "booking.expired", event.venueName(), slot(event, w)));
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
		boolean toPlayer = event.playerUserId() != null && !event.playerUserId().equals(event.actorUserId());
		switch (event.type()) {
			case CREATED -> {
				if (toPlayer) {
					tell(event.playerUserId(), NotificationType.RECURRING_BOOKING_CREATED, data, w -> {
						String days = skippedDays(event, w);
						return new Notice(w.get("recurring.created.title", event.venueName()), days.isEmpty()
								? w.get("recurring.created.body", weekly(event, w), event.courtName(), Messages.date(event.startsOn(), w))
								: w.get("recurring.created.bodySkipped", weekly(event, w), event.courtName(),
										Messages.date(event.startsOn(), w), days));
					});
				}
			}
			case CANCELLED -> {
				if (toPlayer) {
					tell(event.playerUserId(), NotificationType.RECURRING_BOOKING_CANCELLED, data,
							w -> new Notice(w.get("recurring.cancelled.title", event.venueName()),
									w.get("recurring.cancelled.body", weekly(event, w), event.courtName())));
				}
			}
			case SKIPPED -> {
				String body = event.courtName() + " was taken or blocked on " + skipped + ", so " + weekly
						+ " wasn't booked those weeks.";
				this.access.usersWith(event.venueId(), VenuePermission.MANAGE_BOOKINGS)
					.forEach(userId -> this.notifications.send(userId, NotificationType.RECURRING_BOOKING_SKIPPED,
							"Weekly slot for " + event.customerName() + " not booked", body, data));
				if (event.playerUserId() != null) {
					tell(event.playerUserId(), NotificationType.RECURRING_BOOKING_SKIPPED, data,
							w -> new Notice(w.get("recurring.skipped.title", event.venueName()),
									w.get("recurring.skipped.body", event.courtName(), skippedDays(event, w), weekly(event, w))));
				}
			}
		}
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(PaymentEvent event) {
		String slot = Messages.when(event.startsAt());
		String amount = Messages.rupees(event.amount());
		UUID player = event.playerUserId();
		Map<String, String> data = Map.of("bookingId", event.bookingId().toString(), "venueId", event.venueId().toString());
		switch (event.type()) {
			case SUBMITTED -> this.access.usersWith(event.venueId(), VenuePermission.MANAGE_BOOKINGS)
				.forEach(userId -> this.notifications.send(userId, NotificationType.PAYMENT_TO_VERIFY, "Payment to verify",
						event.customerName() + " paid " + amount + " by " + event.method() + " for " + slot
								+ ". Check your wallet, then approve or reject.",
						data));
			case RECEIVED -> {
				if (player != null && !player.equals(event.actorUserId())) {
					String body = (event.bookingStatus() == BookingStatus.CONFIRMED) ? "payment.received.bodyConfirmed"
							: "payment.received.body";
					tell(player, NotificationType.PAYMENT_RECEIVED, data, w -> new Notice(w.get("payment.received.title"),
							w.get(body, event.venueName(), Messages.rupees(event.amount(), w), Messages.when(event.startsAt(), w))));
				}
			}
			case REJECTED -> {
				if (player != null) {
					tell(player, NotificationType.PAYMENT_REJECTED, data, w -> {
						String paid = Messages.rupees(event.amount(), w);
						return new Notice(w.get("payment.rejected.title"), (event.holdExpiresAt() != null)
								? w.get("payment.rejected.bodyDeadline", event.venueName(), paid, event.reason(),
										Messages.time(event.holdExpiresAt().atZone(event.startsAt().getOffset()).toOffsetDateTime(), w))
								: w.get("payment.rejected.body", event.venueName(), paid, event.reason()));
					});
				}
			}
			case REFUND_DUE -> {
				if (player != null) {
					tell(player, NotificationType.REFUND_DUE, data, w -> notice(w, "refund.due", event.venueName(),
							Messages.rupees(event.amount(), w), Messages.when(event.startsAt(), w)));
				}
				this.access.usersWith(event.venueId(), VenuePermission.MANAGE_BOOKINGS)
					.forEach(userId -> this.notifications.send(userId, NotificationType.REFUND_TO_PAY, "Refund to pay",
							event.customerName() + " is owed " + amount + " for " + slot + " (" + event.reason() + ").",
							data));
			}
			case REFUNDED -> {
				if (player != null) {
					tell(player, NotificationType.REFUND_SENT, data, w -> notice(w, "refund.sent", event.venueName(),
							Messages.rupees(event.amount(), w), Messages.when(event.startsAt(), w)));
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
					tell(event.playerUserId(), NotificationType.MEMBERSHIP_ACTIVATED, data, w -> notice(w,
							"membership.active", event.planName(), event.venueName(), Messages.date(event.expiresOn(), w)));
				}
			}
			case REJECTED -> {
				if (event.playerUserId() != null) {
					tell(event.playerUserId(), NotificationType.PAYMENT_REJECTED, data,
							w -> new Notice(w.get("payment.rejected.title"), w.get("membership.rejected.body", event.venueName(),
									Messages.rupees(event.amount(), w), event.planName(), event.reason())));
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
			case REPLIED -> tell(event.authorUserId(), NotificationType.REVIEW_REPLY, data,
					w -> new Notice(w.get("review.replied.title", event.venueName()), w.get("review.replied.body")));
		}
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(TeamEvent event) {
		Map<String, String> data = Map.of("teamId", event.teamId().toString(), "teamSlug", event.teamSlug());
		switch (event.type()) {
			case INVITED -> tell(event.userId(), NotificationType.TEAM_INVITE, data,
					w -> new Notice(w.get("team.invited.title", event.teamName()), w.get("team.invited.body", event.actorName())));
			case JOINED -> this.teams.squad(event.teamId())
				.stream()
				.filter(userId -> !userId.equals(event.userId()))
				.forEach(userId -> tell(userId, NotificationType.TEAM_MEMBER_JOINED, data,
						w -> new Notice(w.get("team.joined.title", event.userName(), event.teamName()), w.get("team.joined.body"))));
			case REMOVED -> tell(event.userId(), NotificationType.TEAM_MEMBER_REMOVED, data,
					w -> new Notice(w.get("team.removed.title", event.teamName()), w.get("team.removed.body", event.actorName())));
			case CAPTAIN_CHANGED -> tell(event.userId(), NotificationType.TEAM_CAPTAIN_CHANGED, data,
					w -> new Notice(w.get("team.captain.title", event.teamName()), w.get("team.captain.body", event.actorName())));
		}
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(MatchEvent event) {
		Map<String, String> data = Map.of("matchPostId", event.postId().toString());
		String opened = (event.postType() == MatchPostType.FIND_OPPONENT) ? "match.withdrew.match" : "match.withdrew.game";
		for (UUID recipient : event.recipients()) {
			tell(recipient, matchType(event), data, w -> {
				String match = w.get("match.place", event.place(), Messages.when(event.startsAt(), w));
				return switch (event.type()) {
					case REQUESTED -> new Notice(w.get("match.requested.title", event.actorName()), w.get("match.requested.body", match));
					case ACCEPTED -> new Notice(w.get("match.accepted.title"), w.get("match.accepted.body", event.actorName(), match));
					case DECLINED -> new Notice(w.get("match.declined.title"), w.get("match.declined.body", match));
					case WITHDRAWN -> new Notice(w.get("match.withdrew.title", event.actorName()), w.get(opened, match));
					case CANCELLED -> new Notice(w.get("match.cancelled.title"), w.get("match.cancelled.body", event.actorName(), match));
				};
			});
		}
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(SplitEvent event) {
		Map<String, String> data = Map.of("bookingId", event.bookingId().toString());
		NotificationType type = (event.type() == SplitEvent.Type.ADDED) ? NotificationType.SPLIT_ADDED : NotificationType.SPLIT_RESULT;
		tell(event.recipientUserId(), type, data, w -> {
			String game = w.get("split.game", event.venueName(), Messages.when(event.startsAt(), w));
			if (event.type() == SplitEvent.Type.ADDED) {
				return new Notice(w.get("split.added.title", event.bookerName()), w.get("split.added.body", game, event.sideName()));
			}
			String outcome = event.draw() ? w.get("split.draw") : w.get("split.won", event.winnerName());
			return new Notice(outcome, (event.share().signum() > 0)
					? w.get("split.share", game, Messages.rupees(event.share(), w), event.bookerName()) : w.get("split.free", game));
		});
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(TournamentEvent event) {
		Map<String, String> data = Map.of("tournamentId", event.tournamentId().toString(), "tournamentSlug",
				event.slug());
		String name = event.tournamentName();
		for (UUID recipient : event.recipients()) {
			switch (event.type()) {
				// To the venue's staff
				case REGISTERED -> this.notifications.send(recipient, NotificationType.TOURNAMENT_REGISTRATION,
						event.teamName() + " signed up", "New entry for " + name + ".", data);
				case ENTRY_CONFIRMED -> tell(recipient, NotificationType.TOURNAMENT_ENTRY_CONFIRMED, data,
						w -> new Notice(w.get("tournament.confirmed.title", event.teamName(), name), w.get("tournament.confirmed.body")));
				case PROMOTED -> tell(recipient, NotificationType.TOURNAMENT_PLACE_OFFERED, data,
						w -> new Notice(w.get("tournament.promoted.title", name), w.get("tournament.promoted.body", event.teamName())));
				case FIXTURES_DRAWN -> tell(recipient, NotificationType.TOURNAMENT_FIXTURES, data,
						w -> new Notice(w.get("tournament.fixtures.title"), w.get("tournament.fixtures.body", name)));
				case MATCH_SCHEDULED -> tell(recipient, NotificationType.TOURNAMENT_MATCH_SCHEDULED, data,
						w -> new Notice(w.get("tournament.match.title", event.teamName(), event.opponentName()),
								w.get("tournament.match.body", name, event.courtName(), Messages.when(event.at(), w))));
				case CANCELLED -> tell(recipient, NotificationType.TOURNAMENT_CANCELLED, data,
						w -> notice(w, "tournament.cancelled", name, event.venueName()));
			}
		}
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(EntryPaymentEvent event) {
		Map<String, String> data = Map.of("tournamentRegistrationId", event.registrationId().toString());
		String amount = Messages.rupees(event.amount());
		switch (event.type()) {
			case SUBMITTED -> this.access.usersWith(event.venueId(), VenuePermission.MANAGE_BOOKINGS)
				.forEach(userId -> this.notifications.send(userId, NotificationType.PAYMENT_TO_VERIFY, "Payment to verify",
						event.teamName() + " paid the " + amount + " entry for " + event.tournamentName() + " by "
								+ event.method() + ". Check your wallet, then approve or reject.",
						data));
			case REJECTED -> tell(event.registeredBy(), NotificationType.PAYMENT_REJECTED, data,
					w -> new Notice(w.get("payment.rejected.title"), w.get("entry.rejected.body", Messages.rupees(event.amount(), w),
							event.tournamentName(), event.reason())));
			default -> {
			}
		}
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(BillingEvent event) {
		Map<String, String> data = Map.of("venueId", event.venueId().toString());
		switch (event.type()) {
			case INVOICE_DUE -> this.notifications.send(event.ownerId(), NotificationType.INVOICE_DUE,
					"Invoice for " + event.planName(), Messages.rupees(event.amount()) + " for " + event.venueName()
							+ " is due " + Messages.date(event.date()) + ".",
					data);
			case SUBSCRIPTION_ACTIVE -> this.notifications.send(event.ownerId(), NotificationType.SUBSCRIPTION_ACTIVE,
					event.planName() + " is active", event.venueName() + " is paid up until " + Messages.date(event.date())
							+ ".",
					data);
			case SUBSCRIPTION_PAST_DUE -> this.notifications.send(event.ownerId(), NotificationType.SUBSCRIPTION_PAST_DUE,
					"Renewal not paid", "Pay by " + Messages.date(event.date()) + " to keep " + event.planName() + " for "
							+ event.venueName() + ".",
					data);
			case SUBSCRIPTION_ENDED -> this.notifications.send(event.ownerId(), NotificationType.SUBSCRIPTION_ENDED,
					event.venueName() + " is on the Free plan", "Your " + event.planName() + " subscription has ended.",
					data);
			case FEATURED_CONFIRMED -> this.notifications.send(event.ownerId(), NotificationType.FEATURED_CONFIRMED,
					"Featured listing confirmed", event.venueName() + " is featured from " + Messages.date(event.date())
							+ ".",
					data);
		}
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(PlatformPaymentRejected event) {
		if (event.deposit()) {
			tell(event.payerUserId(), NotificationType.PAYMENT_REJECTED, Map.of(), w -> new Notice(w.get("payment.rejected.title"),
					w.get("deposit.rejected.body", Messages.rupees(event.amount(), w), event.reason())));
			return;
		}
		this.notifications.send(event.payerUserId(), NotificationType.PAYMENT_REJECTED, "Payment not found",
				"FutSlot couldn't find your " + Messages.rupees(event.amount()) + " payment for " + event.what() + ": "
						+ event.reason() + ".",
				Map.of());
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(DepositEvent event) {
		switch (event.type()) {
			case TOPPED_UP -> tell(event.userId(), NotificationType.DEPOSIT_TOPPED_UP, Map.of(),
					w -> new Notice(w.get("deposit.toppedUp.title"), w.get("deposit.toppedUp.body",
							Messages.rupees(event.amount(), w), Messages.rupees(event.balance(), w))));
			case FEE_TAKEN -> tell(event.userId(), NotificationType.DEPOSIT_FEE_TAKEN, Map.of(), w -> {
				String taken = Messages.rupees(event.amount(), w);
				String why = (event.feePercent() != null)
						? w.get("deposit.fee.late", event.reference(), w.format().number(event.feePercent()))
						: w.get("deposit.fee.noShow", event.reference());
				return new Notice(w.get("deposit.fee.title", taken),
						w.get("deposit.fee.body", event.venueName(), taken, why, Messages.rupees(DepositService.AMOUNT, w)));
			});
			case WITHDRAWAL_PAID -> tell(event.userId(), NotificationType.DEPOSIT_RETURNED, Map.of(),
					w -> new Notice(w.get("deposit.returned.title"),
							w.get("deposit.returned.body", Messages.rupees(event.amount(), w), event.detail())));
			case PAYOUT_SENT -> this.notifications.send(event.userId(), NotificationType.DEPOSIT_PAYOUT_SENT,
					"Deposit fees paid", "FutSlot sent " + event.venueName() + " " + Messages.rupees(event.amount())
							+ " for " + event.detail() + ".",
					Map.of());
		}
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(SlotFreed event) {
		Map<String, String> data = Map.of("venueId", event.venueId().toString(), "venueSlug", event.venueSlug(),
				"courtId", event.courtId().toString(), "startsAt", event.startsAt().toString());
		for (UUID recipient : event.recipients()) {
			tell(recipient, NotificationType.WAITLIST_SLOT_FREE, data, w -> new Notice(w.get("waitlist.free.title"),
					w.get("waitlist.free.body", event.venueName(), event.courtName(), Messages.when(event.startsAt(), w))));
		}
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(ChatMessageSent event) {
		Map<String, String> data = Map.of("conversationId", event.conversationId().toString(), "venueId",
				event.venueId().toString(), "side", event.toVenue() ? "VENUE" : "PLAYER");
		for (UUID recipient : event.recipients()) {
			this.notifications.send(recipient, NotificationType.CHAT_MESSAGE, event.fromName(),
					Messages.excerpt(event.preview()), data);
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
		if (event.source().byPlayer()) {
			if (event.status() == BookingStatus.PENDING_PAYMENT) {
				toPlayer(event, NotificationType.BOOKING_PAYMENT_DUE, data, w -> notice(w, "booking.paymentDue",
						Messages.rupees(event.advanceRequired(), w),
						Messages.time(event.holdExpiresAt().atZone(event.startsAt().getOffset()).toOffsetDateTime(), w),
						event.venueName(), slot(event, w)));
			}
			else {
				toPlayer(event, NotificationType.BOOKING_CONFIRMED, data,
						w -> notice(w, "booking.confirmed", event.venueName(), slot(event, w), event.reference()));
			}
			toVenue(event, NotificationType.NEW_BOOKING, "New booking", event.customerName() + " booked " + slot + ".",
					data);
		}
		else {
			toPlayer(event, NotificationType.BOOKING_CREATED_BY_VENUE, data, w -> new Notice(
					w.get("booking.byVenue.title", event.venueName()), w.get("booking.byVenue.body", slot(event, w), event.reference())));
		}
	}

	private void cancelled(BookingEvent event, String slot, Map<String, String> data) {
		String reason = (event.reason() != null && !event.reason().isBlank()) ? " Reason: " + event.reason() : "";
		if (Objects.equals(event.actorUserId(), event.playerUserId())) {
			toVenue(event, NotificationType.BOOKING_CANCELLED, "Booking cancelled",
					event.customerName() + " cancelled " + slot + "." + reason, data);
		}
		else {
			boolean given = event.reason() != null && !event.reason().isBlank();
			toPlayer(event, NotificationType.BOOKING_CANCELLED, data, w -> new Notice(
					w.get("booking.cancelled.title", event.venueName()), given
							? w.get("booking.cancelled.bodyWithReason", slot(event, w), event.reason())
							: w.get("booking.cancelled.body", slot(event, w))));
		}
	}

	private void toPlayer(BookingEvent event, NotificationType type, Map<String, String> data, Function<Wording, Notice> notice) {
		if (event.playerUserId() != null) {
			tell(event.playerUserId(), type, data, notice);
		}
	}

	/** Sends a player a notification worded in their language. */
	private void tell(UUID userId, NotificationType type, Map<String, String> data, Function<Wording, Notice> notice) {
		Notice worded = notice.apply(this.words.in(this.users.language(userId)));
		this.notifications.send(userId, type, worded.title(), worded.body(), data);
	}

	/** The {@code key}.title and {@code key}.body messages, both given the same values. */
	private static Notice notice(Wording words, String key, Object... values) {
		return new Notice(words.get(key + ".title", values), words.get(key + ".body", values));
	}

	/** "Court A, Fri 9 Oct, 18:00". */
	private static String slot(BookingEvent event, Wording words) {
		return words.get("booking.slot", event.courtName(), Messages.when(event.startsAt(), words));
	}

	private static String weekly(RecurringBookingEvent event, Wording words) {
		return Messages.weekly(event.dayOfWeek(), event.startTime(), event.endTime(), words);
	}

	private static String skippedDays(RecurringBookingEvent event, Wording words) {
		return event.skippedDates().stream().map(day -> Messages.day(day, words)).collect(Collectors.joining(", "));
	}

	private static NotificationType matchType(MatchEvent event) {
		return switch (event.type()) {
			case REQUESTED -> NotificationType.MATCH_REQUEST;
			case ACCEPTED -> NotificationType.MATCH_REQUEST_ACCEPTED;
			case DECLINED -> NotificationType.MATCH_REQUEST_DECLINED;
			case WITHDRAWN -> NotificationType.MATCH_PLAYER_WITHDREW;
			case CANCELLED -> NotificationType.MATCH_POST_CANCELLED;
		};
	}

	private record Notice(String title, String body) {
	}

	/** The owner and staff who handle bookings, except whoever made the change. */
	private void toVenue(BookingEvent event, NotificationType type, String title, String body, Map<String, String> data) {
		this.access.usersWith(event.venueId(), VenuePermission.MANAGE_BOOKINGS)
			.stream()
			.filter(userId -> !userId.equals(event.actorUserId()))
			.forEach(userId -> this.notifications.send(userId, type, title, body, data));
	}

}
