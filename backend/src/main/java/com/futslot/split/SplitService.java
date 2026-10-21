package com.futslot.split;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.booking.Booking;
import com.futslot.booking.BookingLookup;
import com.futslot.booking.BookingResponse;
import com.futslot.booking.BookingStatus;
import com.futslot.common.ApiException;
import com.futslot.common.PhoneNumbers;
import com.futslot.common.Wording;
import com.futslot.common.Words;
import com.futslot.user.User;
import com.futslot.user.UserRepository;

/**
 * Sharing a booking's cost between the players, including "baazi" where the result decides who pays.
 * The booker pays the venue and sets up the split; FutSlot works out the shares and tracks who has paid
 * the booker back. No money passes through FutSlot.
 */
@Service
class SplitService {

	static final int MAX_PLAYERS = 20;

	private final BookingSplitRepository splits;

	private final SplitPlayerRepository players;

	private final BookingLookup bookings;

	private final UserRepository users;

	private final ApplicationEventPublisher events;

	private final Words words;

	private final Clock clock;

	SplitService(BookingSplitRepository splits, SplitPlayerRepository players, BookingLookup bookings,
			UserRepository users, ApplicationEventPublisher events, Words words, Clock clock) {
		this.splits = splits;
		this.players = players;
		this.bookings = bookings;
		this.users = users;
		this.events = events;
		this.words = words;
		this.clock = clock;
	}

	/** Sets up the split (adding the booker as the first player) or changes its rules before the result. */
	@Transactional
	SplitView setUp(UUID userId, UUID bookingId, SplitRequests.Rules request) {
		Booking booking = bookersBooking(userId, bookingId);
		if (request.rule() == SplitRule.LOSERS_PAY_PERCENT && request.losersPercent() == null) {
			throw badRequest("PERCENT_REQUIRED", "Say what share the losing side pays, e.g. 70");
		}
		BookingSplit split = this.splits.findById(bookingId).orElse(null);
		if (split != null) {
			requireNoResult(split);
			split.apply(request);
			return view(booking, split, userId);
		}
		// The sides are "Team A" and "Team B" in the booker's language until they name them
		Wording w = this.words.forRequest();
		split = new BookingSplit(bookingId, w.get("split.sideA"), w.get("split.sideB"));
		split.apply(request);
		split = this.splits.save(split);
		User booker = this.users.findById(userId).orElseThrow();
		this.players.save(new SplitPlayer(bookingId, booker, null, null,
				(request.bookerSide() != null) ? request.bookerSide() : Side.A));
		return view(booking, split, userId);
	}

	/** For the booker and every player with the app. */
	@Transactional(readOnly = true)
	SplitView get(UUID userId, UUID bookingId) {
		BookingSplit split = this.splits.findById(bookingId).orElseThrow(SplitService::splitNotFound);
		Booking booking = this.bookings.byId(bookingId);
		if (!isBooker(booking, userId) && !this.players.existsByBookingIdAndUserId(bookingId, userId)) {
			throw splitNotFound();
		}
		return view(booking, split, userId);
	}

	@Transactional
	SplitView addPlayer(UUID userId, UUID bookingId, SplitRequests.AddPlayer request) {
		Booking booking = bookersBooking(userId, bookingId);
		BookingSplit split = split(bookingId);
		requireNoResult(split);
		if (this.players.countByBookingId(bookingId) >= MAX_PLAYERS) {
			throw new ApiException(HttpStatus.CONFLICT, "TOO_MANY_PLAYERS", "A game can have at most " + MAX_PLAYERS + " players", MAX_PLAYERS);
		}
		String phone = (request.phone() != null && !request.phone().isBlank()) ? PhoneNumbers.normalize(request.phone())
				: null;
		User user = (phone != null) ? this.users.findByPhone(phone).orElse(null) : null;
		if (user != null) {
			if (this.players.existsByBookingIdAndUserId(bookingId, user.getId())) {
				throw new ApiException(HttpStatus.CONFLICT, "ALREADY_ADDED", user.getFullName() + " is already in the game");
			}
		}
		else {
			if (request.name() == null || request.name().isBlank()) {
				throw badRequest("NAME_REQUIRED", "Enter their name");
			}
			if (phone != null && this.players.existsByBookingIdAndPhone(bookingId, phone)) {
				throw new ApiException(HttpStatus.CONFLICT, "ALREADY_ADDED", "Someone with that number is already in the game");
			}
		}
		this.players.save(new SplitPlayer(bookingId, user, (request.name() != null) ? request.name().strip() : null,
				phone, request.side()));
		if (user != null) {
			publish(SplitEvent.Type.ADDED, booking, split, user.getId(), request.side(), null, null);
		}
		return view(booking, split, userId);
	}

	@Transactional
	SplitView movePlayer(UUID userId, UUID bookingId, UUID playerId, Side side) {
		Booking booking = bookersBooking(userId, bookingId);
		BookingSplit split = split(bookingId);
		requireNoResult(split);
		player(bookingId, playerId).setSide(side);
		return view(booking, split, userId);
	}

	@Transactional
	SplitView removePlayer(UUID userId, UUID bookingId, UUID playerId) {
		Booking booking = bookersBooking(userId, bookingId);
		BookingSplit split = split(bookingId);
		requireNoResult(split);
		SplitPlayer player = player(bookingId, playerId);
		if (player.is(userId)) {
			throw badRequest("BOOKER_STAYS", "You booked it, so you're in the game");
		}
		this.players.delete(player);
		return view(booking, split, userId);
	}

	/** Once the game has started: fixes each player's share. The booker's own share counts as paid. */
	@Transactional
	SplitView recordResult(UUID userId, UUID bookingId, MatchResult result) {
		Booking booking = bookersBooking(userId, bookingId);
		BookingSplit split = split(bookingId);
		requireNoResult(split);
		Instant now = this.clock.instant();
		if (now.isBefore(booking.getStartsAt())) {
			throw new ApiException(HttpStatus.CONFLICT, "MATCH_NOT_STARTED", "Record the result after the game");
		}
		MatchResult outcome = (split.getRule() == SplitRule.EQUAL && result == null) ? MatchResult.DRAW : result;
		if (outcome == null) {
			throw badRequest("RESULT_REQUIRED", "Who won?");
		}
		List<SplitPlayer> roster = this.players.findByBookingIdOrderByCreatedAtAscIdAsc(bookingId);
		List<Side> sides = roster.stream().map(SplitPlayer::getSide).toList();
		if (split.getRule() != SplitRule.EQUAL && outcome != MatchResult.DRAW
				&& (SplitCalculator.count(sides, Side.A) == 0 || SplitCalculator.count(sides, Side.B) == 0)) {
			throw badRequest("SIDES_INCOMPLETE", "Put players on both sides first");
		}
		List<BigDecimal> shares = SplitCalculator.shares(split.getRule(), split.getLosersPercent(),
				booking.getTotalAmount(), split.getExtrasAmount(), outcome, sides);
		split.record(outcome, now);
		for (int i = 0; i < roster.size(); i++) {
			SplitPlayer player = roster.get(i);
			player.owe(shares.get(i), now);
			if (player.is(userId)) {
				player.settle(now);
			}
			else if (player.getUser() != null) {
				publish(SplitEvent.Type.RESULT, booking, split, player.getUser().getId(), player.getSide(), outcome,
						shares.get(i));
			}
		}
		return view(booking, split, userId);
	}

	/** Entered the wrong result: undone as long as nobody has been marked as paid yet. */
	@Transactional
	SplitView undoResult(UUID userId, UUID bookingId) {
		Booking booking = bookersBooking(userId, bookingId);
		BookingSplit split = split(bookingId);
		if (split.getResult() == null) {
			throw new ApiException(HttpStatus.CONFLICT, "NO_RESULT", "No result has been recorded");
		}
		List<SplitPlayer> roster = this.players.findByBookingIdOrderByCreatedAtAscIdAsc(bookingId);
		if (roster.stream().anyMatch(player -> !player.is(userId) && player.isSettled()
				&& player.getShareAmount().signum() > 0)) {
			throw new ApiException(HttpStatus.CONFLICT, "ALREADY_SETTLING",
					"Some players are already marked as paid. Unmark them first");
		}
		split.clearResult();
		roster.forEach(SplitPlayer::reset);
		return view(booking, split, userId);
	}

	@Transactional
	SplitView markPaid(UUID userId, UUID bookingId, UUID playerId, boolean paid) {
		Booking booking = bookersBooking(userId, bookingId);
		BookingSplit split = split(bookingId);
		if (split.getResult() == null) {
			throw new ApiException(HttpStatus.CONFLICT, "NO_RESULT", "Record the result first");
		}
		SplitPlayer player = player(bookingId, playerId);
		if (paid) {
			player.settle(this.clock.instant());
		}
		else {
			player.unsettle();
		}
		return view(booking, split, userId);
	}

	@Transactional(readOnly = true)
	List<OwedShare> owed(UUID userId) {
		List<OwedShare> owed = new ArrayList<>();
		for (SplitPlayer player : this.players.findOwedBy(userId)) {
			Booking booking = this.bookings.byId(player.getBookingId());
			BookingSplit split = split(player.getBookingId());
			owed.add(new OwedShare(booking.getId(), booking.getVenue().getName(), startsAt(booking),
					split.sideName(player.getSide()), split.getResult(), player.getShareAmount(), booker(booking)));
		}
		return owed;
	}

	private SplitView view(Booking booking, BookingSplit split, UUID viewerId) {
		boolean viewerIsBooker = isBooker(booking, viewerId);
		List<SplitPlayer> roster = this.players.findByBookingIdOrderByCreatedAtAscIdAsc(booking.getId());
		UUID bookerId = booking.getCustomer().getUserId();
		List<SplitView.Player> shown = roster.stream()
			.map(player -> new SplitView.Player(player.getId(), player.displayName(), player.getSide(),
					player.is(bookerId), player.is(viewerId), player.getUser() != null,
					viewerIsBooker ? player.contactPhone() : null,
					(split.getResult() != null) ? player.getShareAmount() : null, player.isSettled(),
					player.getSettledAt()))
			.toList();
		BigDecimal court = booking.getTotalAmount();
		BigDecimal total = court.add(split.getExtrasAmount());
		BigDecimal collected = BigDecimal.ZERO;
		BigDecimal outstanding = BigDecimal.ZERO;
		List<SplitView.Outcome> outcomes = null;
		if (split.getResult() != null) {
			for (SplitPlayer player : roster) {
				if (player.isSettled()) {
					collected = collected.add(player.getShareAmount());
				}
				else {
					outstanding = outstanding.add(player.getShareAmount());
				}
			}
		}
		else {
			List<Side> sides = roster.stream().map(SplitPlayer::getSide).toList();
			outcomes = new ArrayList<>();
			for (MatchResult result : MatchResult.values()) {
				outcomes.add(outcome(split, court, result, sides));
			}
		}
		return new SplitView(booking.getId(), booking.getReference(), booking.getVenue().getName(),
				booking.getCourt().getName(), startsAt(booking), booker(booking), split.getRule(),
				split.getLosersPercent(), court, split.getExtrasAmount(), split.getExtrasNote(), total,
				split.getSideAName(), split.getSideBName(), shown, split.getResult(), split.getResultRecordedAt(),
				outcomes, collected, outstanding, viewerIsBooker);
	}

	private static SplitView.Outcome outcome(BookingSplit split, BigDecimal court, MatchResult result, List<Side> sides) {
		List<BigDecimal> shares = SplitCalculator.shares(split.getRule(), split.getLosersPercent(), court,
				split.getExtrasAmount(), result, sides);
		BigDecimal sideA = BigDecimal.ZERO;
		BigDecimal sideB = BigDecimal.ZERO;
		for (int i = 0; i < sides.size(); i++) {
			if (sides.get(i) == Side.A) {
				sideA = sideA.add(shares.get(i));
			}
			else {
				sideB = sideB.add(shares.get(i));
			}
		}
		return new SplitView.Outcome(result, sideA, each(sideA, SplitCalculator.count(sides, Side.A)), sideB,
				each(sideB, SplitCalculator.count(sides, Side.B)));
	}

	private static BigDecimal each(BigDecimal total, int players) {
		return (players > 0) ? total.divide(BigDecimal.valueOf(players), 0, RoundingMode.HALF_UP) : null;
	}

	private void publish(SplitEvent.Type type, Booking booking, BookingSplit split, UUID recipient, Side side,
			MatchResult result, BigDecimal share) {
		String winner = (result != null && result.loser() != null) ? split.sideName(result.loser().other()) : null;
		this.events.publishEvent(new SplitEvent(type, booking.getId(), recipient, booker(booking).name(),
				booking.getVenue().getName(), startsAt(booking), split.sideName(side), winner,
				result == MatchResult.DRAW, share));
	}

	/** The booker's booking, while it's still on or has been played. */
	private Booking bookersBooking(UUID userId, UUID bookingId) {
		Booking booking = this.bookings.forPlayer(userId, bookingId);
		if (!booking.isLive() && booking.getStatus() != BookingStatus.COMPLETED) {
			throw new ApiException(HttpStatus.CONFLICT, "BOOKING_NOT_ACTIVE", "This booking is " + booking.getStatus());
		}
		return booking;
	}

	private static boolean isBooker(Booking booking, UUID userId) {
		return userId.equals(booking.getCustomer().getUserId());
	}

	private SplitView.Person booker(Booking booking) {
		UUID bookerId = booking.getCustomer().getUserId();
		User user = (bookerId != null) ? this.users.findById(bookerId).orElse(null) : null;
		return (user != null) ? new SplitView.Person(user.getId(), user.getFullName(), user.getPhone())
				: new SplitView.Person(null, booking.getCustomer().getName(), booking.getCustomer().getPhone());
	}

	private static OffsetDateTime startsAt(Booking booking) {
		return BookingResponse.from(booking).startsAt();
	}

	private BookingSplit split(UUID bookingId) {
		return this.splits.findById(bookingId).orElseThrow(SplitService::splitNotFound);
	}

	private SplitPlayer player(UUID bookingId, UUID playerId) {
		return this.players.findByIdAndBookingId(playerId, bookingId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PLAYER_NOT_FOUND", "Player not found"));
	}

	private static void requireNoResult(BookingSplit split) {
		if (split.getResult() != null) {
			throw new ApiException(HttpStatus.CONFLICT, "RESULT_RECORDED",
					"The result is already in. Undo it to change the game");
		}
	}

	private static ApiException splitNotFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "SPLIT_NOT_FOUND", "No split for this booking");
	}

	private static ApiException badRequest(String code, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, code, message);
	}

}
