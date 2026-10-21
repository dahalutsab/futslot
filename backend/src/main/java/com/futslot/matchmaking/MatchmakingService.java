package com.futslot.matchmaking;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.futslot.booking.Booking;
import com.futslot.booking.BookingLookup;
import com.futslot.common.ApiException;
import com.futslot.common.Names;
import com.futslot.common.PageResponse;
import com.futslot.team.PlayerService;
import com.futslot.team.SkillLevel;
import com.futslot.team.Team;
import com.futslot.team.TeamService;
import com.futslot.user.User;
import com.futslot.user.UserRepository;
import com.futslot.venue.Venue;
import com.futslot.venue.VenueRepository;
import com.futslot.venue.VenueStatus;

/**
 * "Need players" and "find an opponent" posts. The poster accepts or declines requests; a post fills
 * when its places are taken and reopens if someone drops out before kick-off. Phone numbers are shared
 * only between the poster and accepted players.
 */
@Service
class MatchmakingService {

	static final int MAX_OPEN_POSTS = 5;

	static final Duration MAX_AHEAD = Duration.ofDays(30);

	/** Matchmaking is city-wide; all venues are in Nepal today. */
	private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kathmandu");

	private final MatchPostRepository posts;

	private final JoinRequestRepository requests;

	private final UserRepository users;

	private final VenueRepository venues;

	private final BookingLookup bookings;

	private final TeamService teams;

	private final PlayerService players;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	MatchmakingService(MatchPostRepository posts, JoinRequestRepository requests, UserRepository users,
			VenueRepository venues, BookingLookup bookings, TeamService teams, PlayerService players,
			ApplicationEventPublisher events, Clock clock) {
		this.posts = posts;
		this.requests = requests;
		this.users = users;
		this.venues = venues;
		this.bookings = bookings;
		this.teams = teams;
		this.players = players;
		this.events = events;
		this.clock = clock;
	}

	@Transactional
	MatchPostView create(UUID userId, MatchPostRequest request) {
		Instant now = this.clock.instant();
		if (this.posts.countOpenBy(userId, now) >= MAX_OPEN_POSTS) {
			throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_POSTS",
					"You can have " + MAX_OPEN_POSTS + " open posts at a time", MAX_OPEN_POSTS);
		}
		Team team = (request.teamId() != null) ? this.teams.led(userId, request.teamId()) : null;
		if (request.type() == MatchPostType.FIND_OPPONENT && team == null) {
			throw badRequest("TEAM_REQUIRED", "Pick the team that's looking for an opponent");
		}
		if (request.type() == MatchPostType.NEED_PLAYERS && request.playersNeeded() == null) {
			throw badRequest("PLAYERS_NEEDED_REQUIRED", "Say how many players you need");
		}
		Venue venue = null;
		String city;
		Instant startsAt;
		if (request.bookingId() != null) {
			Booking booking = this.bookings.forPlayer(userId, request.bookingId());
			if (!booking.isLive()) {
				throw new ApiException(HttpStatus.CONFLICT, "BOOKING_NOT_ACTIVE", "That booking is " + booking.getStatus());
			}
			venue = booking.getVenue();
			city = venue.getCity();
			startsAt = booking.getStartsAt();
		}
		else {
			if (request.startsAt() == null) {
				throw badRequest("START_REQUIRED", "Say when the match starts");
			}
			startsAt = request.startsAt().toInstant();
			if (request.venueId() != null) {
				venue = this.venues.findById(request.venueId())
					.filter(found -> found.getStatus() == VenueStatus.ACTIVE)
					.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VENUE_NOT_FOUND", "Venue not found"));
				city = venue.getCity();
			}
			else if (StringUtils.hasText(request.city())) {
				city = request.city().strip();
			}
			else {
				throw badRequest("PLACE_REQUIRED", "Pick a booking, a venue or a city");
			}
		}
		if (!startsAt.isAfter(now) || startsAt.isAfter(now.plus(MAX_AHEAD))) {
			throw badRequest("INVALID_START", "Matches can be posted up to " + MAX_AHEAD.toDays() + " days ahead")
				.with(MAX_AHEAD.toDays());
		}
		User creator = user(userId);
		MatchPost post = this.posts.save(new MatchPost(request.type(), creator, team, request.bookingId(), venue, city,
				startsAt, (request.type() == MatchPostType.NEED_PLAYERS) ? request.playersNeeded() : null,
				request.skillLevel(), request.costPerPlayer(), text(request.note())));
		return view(post, userId);
	}

	@Transactional(readOnly = true)
	PageResponse<MatchPostView> browse(UUID userId, String city, MatchPostType type, LocalDate date,
			SkillLevel skillLevel, Pageable pageable) {
		Instant now = this.clock.instant();
		Specification<MatchPost> spec = (root, q, cb) -> cb.and(
				cb.equal(root.get("status"), MatchPostStatus.OPEN), cb.greaterThan(root.get("startsAt"), now));
		if (StringUtils.hasText(city)) {
			spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("city")), city.strip().toLowerCase()));
		}
		if (type != null) {
			spec = spec.and((root, q, cb) -> cb.equal(root.get("type"), type));
		}
		if (skillLevel != null) {
			spec = spec.and((root, q, cb) -> cb.or(cb.isNull(root.get("skillLevel")),
					cb.equal(root.get("skillLevel"), skillLevel)));
		}
		if (date != null) {
			Instant from = date.atStartOfDay(DEFAULT_ZONE).toInstant();
			Instant to = date.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant();
			spec = spec.and((root, q, cb) -> cb.and(cb.greaterThanOrEqualTo(root.get("startsAt"), from),
					cb.lessThan(root.get("startsAt"), to)));
		}
		Page<MatchPost> page = this.posts.findAll(spec, pageable);
		return PageResponse.of(page, post -> view(post, userId));
	}

	@Transactional(readOnly = true)
	MatchPostView get(UUID userId, UUID postId) {
		return view(post(postId), userId);
	}

	@Transactional(readOnly = true)
	PageResponse<MatchPostView> mine(UUID userId, Pageable pageable) {
		return PageResponse.of(this.posts.findByCreatedByIdOrderByStartsAtDesc(userId, pageable),
				post -> view(post, userId));
	}

	@Transactional(readOnly = true)
	PageResponse<MatchPostView> myRequests(UUID userId, Pageable pageable) {
		return PageResponse.of(this.requests.findByUserIdOrderByCreatedAtDesc(userId, pageable),
				request -> view(request.getPost(), userId));
	}

	@Transactional
	MatchPostView ask(UUID userId, UUID postId, JoinRequestBody body) {
		MatchPost post = this.posts.lockById(postId).orElseThrow(MatchmakingService::postNotFound);
		Instant now = this.clock.instant();
		if (!post.isOpenAt(now)) {
			throw new ApiException(HttpStatus.CONFLICT, "POST_NOT_OPEN", "This post is " + post.statusAt(now));
		}
		if (post.getCreatedBy().getId().equals(userId)) {
			throw badRequest("OWN_POST", "That's your own post");
		}
		Team team = null;
		if (post.getType() == MatchPostType.FIND_OPPONENT) {
			if (body.teamId() == null) {
				throw badRequest("TEAM_REQUIRED", "Pick the team you'd play with");
			}
			team = this.teams.led(userId, body.teamId());
			if (team.getId().equals(post.getTeam().getId())) {
				throw badRequest("OWN_TEAM", "A team can't play itself");
			}
		}
		JoinRequest existing = this.requests.findByPostIdAndUserId(postId, userId).orElse(null);
		if (existing == null) {
			this.requests.save(new JoinRequest(post, user(userId), team, text(body.message())));
		}
		else if (existing.getStatus() == JoinRequestStatus.WITHDRAWN) {
			existing.renew(team, text(body.message()));
		}
		else if (existing.getStatus() == JoinRequestStatus.DECLINED) {
			throw new ApiException(HttpStatus.CONFLICT, "REQUEST_DECLINED", "The poster already turned this request down");
		}
		else {
			throw new ApiException(HttpStatus.CONFLICT, "ALREADY_REQUESTED", "You've already asked to join");
		}
		publish(MatchEvent.Type.REQUESTED, post, List.of(post.getCreatedBy().getId()), userId);
		return view(post, userId);
	}

	@Transactional
	MatchPostView accept(UUID userId, UUID postId, UUID requestId) {
		MatchPost post = ownPost(userId, postId);
		Instant now = this.clock.instant();
		JoinRequest request = this.requests.findByIdAndPostId(requestId, postId).orElseThrow(MatchmakingService::requestNotFound);
		if (request.getStatus() != JoinRequestStatus.PENDING) {
			throw new ApiException(HttpStatus.CONFLICT, "REQUEST_NOT_PENDING", "This request is " + request.getStatus());
		}
		if (!post.isOpenAt(now)) {
			throw new ApiException(HttpStatus.CONFLICT, "POST_NOT_OPEN", "This post is " + post.statusAt(now));
		}
		long alreadyAccepted = this.requests.countByPostIdAndStatus(postId, JoinRequestStatus.ACCEPTED);
		request.setStatus(JoinRequestStatus.ACCEPTED);
		publish(MatchEvent.Type.ACCEPTED, post, List.of(request.getUser().getId()), userId);
		if (alreadyAccepted + 1 >= post.capacity()) {
			post.filled();
			// Everyone still waiting is told the places have gone
			List<JoinRequest> waiting = this.requests.findByPostIdAndStatus(postId, JoinRequestStatus.PENDING)
				.stream()
				.filter(other -> !other.getId().equals(requestId))
				.toList();
			waiting.forEach(other -> other.setStatus(JoinRequestStatus.DECLINED));
			if (!waiting.isEmpty()) {
				publish(MatchEvent.Type.DECLINED, post, waiting.stream().map(other -> other.getUser().getId()).toList(),
						userId);
			}
		}
		return view(post, userId);
	}

	/** Declining an accepted player frees their place again. */
	@Transactional
	MatchPostView decline(UUID userId, UUID postId, UUID requestId) {
		MatchPost post = ownPost(userId, postId);
		JoinRequest request = this.requests.findByIdAndPostId(requestId, postId).orElseThrow(MatchmakingService::requestNotFound);
		if (request.getStatus() != JoinRequestStatus.PENDING && request.getStatus() != JoinRequestStatus.ACCEPTED) {
			throw new ApiException(HttpStatus.CONFLICT, "REQUEST_NOT_PENDING", "This request is " + request.getStatus());
		}
		boolean wasAccepted = request.getStatus() == JoinRequestStatus.ACCEPTED;
		request.setStatus(JoinRequestStatus.DECLINED);
		if (wasAccepted) {
			post.reopen(this.clock.instant());
		}
		publish(MatchEvent.Type.DECLINED, post, List.of(request.getUser().getId()), userId);
		return view(post, userId);
	}

	@Transactional
	MatchPostView withdraw(UUID userId, UUID postId) {
		MatchPost post = this.posts.lockById(postId).orElseThrow(MatchmakingService::postNotFound);
		JoinRequest request = this.requests.findByPostIdAndUserId(postId, userId)
			.filter(found -> found.getStatus() == JoinRequestStatus.PENDING
					|| found.getStatus() == JoinRequestStatus.ACCEPTED)
			.orElseThrow(MatchmakingService::requestNotFound);
		boolean wasAccepted = request.getStatus() == JoinRequestStatus.ACCEPTED;
		request.setStatus(JoinRequestStatus.WITHDRAWN);
		if (wasAccepted) {
			post.reopen(this.clock.instant());
			publish(MatchEvent.Type.WITHDRAWN, post, List.of(post.getCreatedBy().getId()), userId);
		}
		return view(post, userId);
	}

	@Transactional
	MatchPostView cancel(UUID userId, UUID postId) {
		MatchPost post = ownPost(userId, postId);
		if (post.getStatus() == MatchPostStatus.CANCELLED) {
			throw new ApiException(HttpStatus.CONFLICT, "POST_CANCELLED", "This post is already cancelled");
		}
		post.cancel();
		List<UUID> affected = this.requests.findByPostIdOrderByCreatedAt(postId)
			.stream()
			.filter(request -> request.getStatus() == JoinRequestStatus.PENDING
					|| request.getStatus() == JoinRequestStatus.ACCEPTED)
			.map(request -> request.getUser().getId())
			.toList();
		if (!affected.isEmpty()) {
			publish(MatchEvent.Type.CANCELLED, post, affected, userId);
		}
		return view(post, userId);
	}

	private MatchPostView view(MatchPost post, UUID viewerId) {
		Instant now = this.clock.instant();
		boolean mine = post.getCreatedBy().getId().equals(viewerId);
		List<JoinRequest> all = this.requests.findByPostIdOrderByCreatedAt(post.getId());
		long taken = all.stream().filter(request -> request.getStatus() == JoinRequestStatus.ACCEPTED).count();
		JoinRequest own = all.stream().filter(request -> request.getUser().getId().equals(viewerId)).findFirst().orElse(null);
		boolean accepted = own != null && own.getStatus() == JoinRequestStatus.ACCEPTED;
		List<MatchPostView.RequestView> requestViews = mine ? all.stream()
			.filter(request -> request.getStatus() != JoinRequestStatus.WITHDRAWN)
			.map(request -> new MatchPostView.RequestView(request.getId(), this.players.card(request.getUser().getId()),
					teamRef(request.getTeam()), request.getMessage(), request.getStatus(),
					(request.getStatus() == JoinRequestStatus.ACCEPTED) ? request.getUser().getPhone() : null,
					request.getCreatedAt()))
			.toList() : null;
		ZoneId zone = (post.getVenue() != null) ? ZoneId.of(post.getVenue().getTimezone()) : DEFAULT_ZONE;
		Venue venue = post.getVenue();
		return new MatchPostView(post.getId(), post.getType(), post.statusAt(now),
				new MatchPostView.Person(post.getCreatedBy().getId(),
						mine ? post.getCreatedBy().getFullName() : Names.shortName(post.getCreatedBy().getFullName())),
				teamRef(post.getTeam()),
				(venue != null) ? new MatchPostView.VenueRef(venue.getId(), venue.getName(), venue.getSlug()) : null,
				post.getCity(), post.getStartsAt().atZone(zone).toOffsetDateTime(), post.getPlayersNeeded(), taken,
				post.getSkillLevel(), post.getCostPerPlayer(), post.getNote(), mine,
				(own != null) ? new MatchPostView.MyRequest(own.getId(), own.getStatus(), teamRef(own.getTeam())) : null,
				accepted ? post.getCreatedBy().getPhone() : null, requestViews, post.getCreatedAt());
	}

	private void publish(MatchEvent.Type type, MatchPost post, List<UUID> recipients, UUID actorUserId) {
		String place = (post.getVenue() != null) ? post.getVenue().getName() : post.getCity();
		ZoneId zone = (post.getVenue() != null) ? ZoneId.of(post.getVenue().getTimezone()) : DEFAULT_ZONE;
		OffsetDateTime startsAt = post.getStartsAt().atZone(zone).toOffsetDateTime();
		this.events.publishEvent(new MatchEvent(type, post.getId(), post.getType(), recipients,
				Names.shortName(user(actorUserId).getFullName()), place, startsAt));
	}

	/** The poster's post, locked so accepting can't overfill it. */
	private MatchPost ownPost(UUID userId, UUID postId) {
		MatchPost post = this.posts.lockById(postId).orElseThrow(MatchmakingService::postNotFound);
		if (!post.getCreatedBy().getId().equals(userId)) {
			throw postNotFound();
		}
		return post;
	}

	private MatchPost post(UUID postId) {
		return this.posts.findById(postId).orElseThrow(MatchmakingService::postNotFound);
	}

	private User user(UUID userId) {
		return this.users.findById(userId).orElseThrow();
	}

	private static MatchPostView.TeamRef teamRef(Team team) {
		return (team != null) ? new MatchPostView.TeamRef(team.getId(), team.getName(), team.getSlug()) : null;
	}

	private static String text(String value) {
		return (value != null && !value.isBlank()) ? value.strip() : null;
	}

	private static ApiException badRequest(String code, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, code, message);
	}

	private static ApiException postNotFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "Match post not found");
	}

	private static ApiException requestNotFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND", "Request not found");
	}

}
