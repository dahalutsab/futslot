package com.futslot.team;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.futslot.common.ApiException;
import com.futslot.common.Names;
import com.futslot.common.PageResponse;
import com.futslot.common.PhoneNumbers;
import com.futslot.common.Slugs;
import com.futslot.user.User;
import com.futslot.user.UserRepository;

/**
 * Teams of app players. The captain and vice-captains run the team; only the captain changes roles,
 * and the captaincy is handed over rather than left empty.
 */
@Service
public class TeamService {

	static final int MAX_SQUAD = 25;

	private static final List<MemberStatus> SQUAD = List.of(MemberStatus.ACTIVE);

	private static final List<MemberStatus> SQUAD_AND_INVITED = List.of(MemberStatus.ACTIVE, MemberStatus.INVITED);

	private final TeamRepository teams;

	private final TeamMemberRepository members;

	private final UserRepository users;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	TeamService(TeamRepository teams, TeamMemberRepository members, UserRepository users,
			ApplicationEventPublisher events, Clock clock) {
		this.teams = teams;
		this.members = members;
		this.users = users;
		this.events = events;
		this.clock = clock;
	}

	@Transactional
	TeamResponse create(UUID userId, TeamRequest request) {
		String slug = Slugs.unique(request.name(), "team", this.teams::existsBySlug);
		Team team = this.teams.save(new Team(slug, userId, request));
		this.members.save(TeamMember.captain(team.getId(), user(userId), this.clock.instant()));
		return view(team, userId);
	}

	@Transactional
	TeamResponse update(UUID userId, UUID teamId, TeamRequest request) {
		Team team = led(userId, teamId);
		team.apply(request);
		return view(team, userId);
	}

	/** Public teams for anyone; private ones only for their members and invitees. */
	@Transactional(readOnly = true)
	TeamResponse get(UUID userId, String slug) {
		Team team = this.teams.findBySlug(slug).orElseThrow(TeamService::teamNotFound);
		if (!team.isPublic() && this.members.findByTeamIdAndUserId(team.getId(), userId)
			.filter(member -> member.getStatus() == MemberStatus.ACTIVE || member.getStatus() == MemberStatus.INVITED)
			.isEmpty()) {
			throw teamNotFound();
		}
		return view(team, userId);
	}

	@Transactional(readOnly = true)
	PageResponse<TeamSummary> search(String city, String query, Pageable pageable) {
		Specification<Team> spec = (root, q, cb) -> cb.isTrue(root.get("isPublic"));
		if (StringUtils.hasText(city)) {
			spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("city")), city.strip().toLowerCase()));
		}
		if (StringUtils.hasText(query)) {
			String pattern = "%" + query.strip().toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";
			spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\'));
		}
		Page<Team> page = this.teams.findAll(spec, pageable);
		return PageResponse.of(page, this::summary);
	}

	/** Teams the user plays for or is invited to; {@code myStatus} tells which. */
	@Transactional(readOnly = true)
	List<TeamResponse> mine(UUID userId) {
		return this.teams.findForUser(userId, SQUAD_AND_INVITED).stream().map(team -> view(team, userId)).toList();
	}

	@Transactional
	TeamResponse invite(UUID userId, UUID teamId, String phone) {
		Team team = led(userId, teamId);
		User invitee = this.users.findByPhone(PhoneNumbers.normalize(phone))
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PLAYER_NOT_FOUND",
					"Nobody has signed up with that number yet. Ask them to join FutSlot first"));
		if (this.members.countByTeamIdAndStatusIn(teamId, SQUAD_AND_INVITED) >= MAX_SQUAD) {
			throw new ApiException(HttpStatus.CONFLICT, "TEAM_FULL", "A team can have at most " + MAX_SQUAD + " players", MAX_SQUAD)
				.variant("max");
		}
		TeamMember member = this.members.findByTeamIdAndUserId(teamId, invitee.getId()).orElse(null);
		if (member == null) {
			this.members.save(TeamMember.invited(teamId, invitee));
		}
		else if (member.getStatus() == MemberStatus.ACTIVE) {
			throw new ApiException(HttpStatus.CONFLICT, "ALREADY_MEMBER", invitee.getFullName() + " is already in the team",
					invitee.getFullName());
		}
		else if (member.getStatus() == MemberStatus.INVITED) {
			throw new ApiException(HttpStatus.CONFLICT, "ALREADY_INVITED", invitee.getFullName() + " is already invited",
					invitee.getFullName());
		}
		else {
			member.reinvite();
		}
		publish(TeamEvent.Type.INVITED, team, invitee, userId);
		return view(team, userId);
	}

	@Transactional
	TeamResponse acceptInvite(UUID userId, UUID teamId) {
		TeamMember member = invitation(userId, teamId);
		Team team = this.teams.findById(teamId).orElseThrow(TeamService::teamNotFound);
		if (this.members.countByTeamIdAndStatusIn(teamId, SQUAD) >= MAX_SQUAD) {
			throw new ApiException(HttpStatus.CONFLICT, "TEAM_FULL", "The team is full");
		}
		member.join(this.clock.instant());
		publish(TeamEvent.Type.JOINED, team, user(userId), userId);
		return view(team, userId);
	}

	@Transactional
	void declineInvite(UUID userId, UUID teamId) {
		this.members.delete(invitation(userId, teamId));
	}

	@Transactional
	void leave(UUID userId, UUID teamId) {
		TeamMember member = this.members.findByTeamIdAndUserId(teamId, userId)
			.filter(TeamMember::isActive)
			.orElseThrow(TeamService::teamNotFound);
		if (member.getRole() == TeamRole.CAPTAIN) {
			throw new ApiException(HttpStatus.CONFLICT, "CAPTAIN_CANNOT_LEAVE",
					"Make someone else captain before you leave");
		}
		member.leave();
	}

	/** Leaders remove members (and cancel invitations); only the captain removes a vice-captain. */
	@Transactional
	TeamResponse removeMember(UUID userId, UUID teamId, UUID memberId) {
		Team team = led(userId, teamId);
		TeamMember actor = this.members.findByTeamIdAndUserId(teamId, userId).orElseThrow();
		TeamMember member = this.members.findByTeamIdAndUserId(teamId, memberId)
			.filter(found -> found.getStatus() == MemberStatus.ACTIVE || found.getStatus() == MemberStatus.INVITED)
			.orElseThrow(TeamService::memberNotFound);
		if (member.getRole() == TeamRole.CAPTAIN
				|| (member.getRole() == TeamRole.VICE_CAPTAIN && actor.getRole() != TeamRole.CAPTAIN)) {
			throw forbidden("Only the captain can remove a vice-captain, and the captain can't be removed").variant("remove");
		}
		if (member.getStatus() == MemberStatus.INVITED) {
			this.members.delete(member);
		}
		else {
			member.remove();
			publish(TeamEvent.Type.REMOVED, team, user(memberId), userId);
		}
		return view(team, userId);
	}

	@Transactional
	TeamResponse changeRole(UUID userId, UUID teamId, UUID memberId, TeamRole role) {
		Team team = this.teams.findById(teamId).orElseThrow(TeamService::teamNotFound);
		TeamMember actor = this.members.findByTeamIdAndUserId(teamId, userId)
			.filter(TeamMember::isActive)
			.orElseThrow(TeamService::teamNotFound);
		if (actor.getRole() != TeamRole.CAPTAIN) {
			throw forbidden("Only the captain can change roles").variant("roles");
		}
		TeamMember member = this.members.findByTeamIdAndUserId(teamId, memberId)
			.filter(TeamMember::isActive)
			.orElseThrow(TeamService::memberNotFound);
		if (member.getUserId().equals(userId)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ROLE_CHANGE", "Hand the captaincy to someone else");
		}
		if (role == TeamRole.CAPTAIN) {
			actor.setRole(TeamRole.VICE_CAPTAIN);
			team.setCaptain(memberId);
			publish(TeamEvent.Type.CAPTAIN_CHANGED, team, user(memberId), userId);
		}
		member.setRole(role);
		return view(team, userId);
	}

	/** For other modules: the team, if {@code userId} is its captain or a vice-captain. */
	@Transactional(readOnly = true)
	public Team led(UUID userId, UUID teamId) {
		Team team = this.teams.findById(teamId).orElseThrow(TeamService::teamNotFound);
		TeamMember member = this.members.findByTeamIdAndUserId(teamId, userId)
			.filter(TeamMember::isActive)
			.orElseThrow(TeamService::teamNotFound);
		if (!member.leads()) {
			throw forbidden("Only the captain or a vice-captain can do this");
		}
		return team;
	}

	/** Active players of the team, for notifying them. */
	@Transactional(readOnly = true)
	public List<UUID> squad(UUID teamId) {
		return this.members.findByTeamIdAndStatusInOrderByCreatedAt(teamId, SQUAD)
			.stream()
			.map(TeamMember::getUserId)
			.toList();
	}

	private TeamMember invitation(UUID userId, UUID teamId) {
		return this.members.findByTeamIdAndUserId(teamId, userId)
			.filter(member -> member.getStatus() == MemberStatus.INVITED)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVITE_NOT_FOUND", "Invitation not found"));
	}

	private TeamResponse view(Team team, UUID viewerId) {
		List<TeamMember> roster = this.members.findByTeamIdAndStatusInOrderByCreatedAt(team.getId(), SQUAD_AND_INVITED);
		Map<UUID, TeamMember> byUser = roster.stream().collect(Collectors.toMap(TeamMember::getUserId, Function.identity()));
		TeamMember me = byUser.get(viewerId);
		boolean insider = me != null && me.isActive();
		boolean leader = me != null && me.leads();
		List<TeamResponse.Member> shown = roster.stream()
			.filter(member -> member.isActive() || leader || member.getUserId().equals(viewerId))
			.sorted((a, b) -> Integer.compare(a.getRole().ordinal(), b.getRole().ordinal()))
			.map(member -> new TeamResponse.Member(member.getUserId(),
					insider ? member.getUser().getFullName() : Names.shortName(member.getUser().getFullName()),
					member.getUser().getAvatarUrl(), member.getRole(), member.getStatus()))
			.toList();
		int squadSize = (int) roster.stream().filter(TeamMember::isActive).count();
		return new TeamResponse(team.getId(), team.getName(), team.getSlug(), team.getLogoUrl(), team.getCity(),
				team.getSkillLevel(), team.isPublic(), team.getCaptainId(), squadSize, shown,
				(me != null) ? me.getRole() : null, (me != null) ? me.getStatus() : null);
	}

	private TeamSummary summary(Team team) {
		return new TeamSummary(team.getId(), team.getName(), team.getSlug(), team.getLogoUrl(), team.getCity(),
				team.getSkillLevel(), this.members.countByTeamIdAndStatusIn(team.getId(), SQUAD));
	}

	private void publish(TeamEvent.Type type, Team team, User user, UUID actorUserId) {
		this.events.publishEvent(new TeamEvent(type, team.getId(), team.getName(), team.getSlug(), user.getId(),
				user.getFullName(), actorUserId, user(actorUserId).getFullName()));
	}

	private User user(UUID userId) {
		return this.users.findById(userId).orElseThrow();
	}

	static ApiException teamNotFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "TEAM_NOT_FOUND", "Team not found");
	}

	private static ApiException memberNotFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "That player isn't in the team");
	}

	private static ApiException forbidden(String message) {
		return new ApiException(HttpStatus.FORBIDDEN, "NOT_TEAM_LEADER", message);
	}

}
