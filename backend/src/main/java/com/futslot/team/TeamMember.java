package com.futslot.team;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.futslot.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A player's place in a team, from invitation on. Rows stay after leaving so history isn't lost. */
@Entity
@Table(name = "team_members")
@IdClass(TeamMember.Key.class)
public class TeamMember {

	@Id
	@Column(name = "team_id")
	private UUID teamId;

	@Id
	@Column(name = "user_id")
	private UUID userId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", insertable = false, updatable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TeamRole role = TeamRole.MEMBER;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MemberStatus status = MemberStatus.INVITED;

	private Instant joinedAt;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected TeamMember() {
	}

	static TeamMember invited(UUID teamId, User user) {
		TeamMember member = new TeamMember();
		member.teamId = teamId;
		member.userId = user.getId();
		member.user = user;
		return member;
	}

	static TeamMember captain(UUID teamId, User user, Instant at) {
		TeamMember member = invited(teamId, user);
		member.role = TeamRole.CAPTAIN;
		member.join(at);
		return member;
	}

	void reinvite() {
		this.status = MemberStatus.INVITED;
		this.role = TeamRole.MEMBER;
		this.joinedAt = null;
	}

	void join(Instant at) {
		this.status = MemberStatus.ACTIVE;
		this.joinedAt = at;
	}

	void leave() {
		this.status = MemberStatus.LEFT;
		this.role = TeamRole.MEMBER;
	}

	void remove() {
		this.status = MemberStatus.REMOVED;
		this.role = TeamRole.MEMBER;
	}

	void setRole(TeamRole role) {
		this.role = role;
	}

	boolean isActive() {
		return this.status == MemberStatus.ACTIVE;
	}

	boolean leads() {
		return isActive() && this.role.leads();
	}

	public UUID getTeamId() {
		return this.teamId;
	}

	public UUID getUserId() {
		return this.userId;
	}

	public User getUser() {
		return this.user;
	}

	public TeamRole getRole() {
		return this.role;
	}

	public MemberStatus getStatus() {
		return this.status;
	}

	public Instant getJoinedAt() {
		return this.joinedAt;
	}

	public record Key(UUID teamId, UUID userId) implements Serializable {

	}

}
