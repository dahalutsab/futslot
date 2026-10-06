package com.futslot.matchmaking;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.futslot.team.Team;
import com.futslot.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A player (or, for FIND_OPPONENT, a team through its leader) asking to take part. One per player per post. */
@Entity
@Table(name = "match_post_responses")
class JoinRequest {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "post_id", updatable = false)
	private MatchPost post;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", updatable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "team_id")
	private Team team;

	private String message;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private JoinRequestStatus status = JoinRequestStatus.PENDING;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected JoinRequest() {
	}

	JoinRequest(MatchPost post, User user, Team team, String message) {
		this.post = post;
		this.user = user;
		renew(team, message);
	}

	/** Asking again after withdrawing. */
	void renew(Team team, String message) {
		this.team = team;
		this.message = message;
		this.status = JoinRequestStatus.PENDING;
	}

	void setStatus(JoinRequestStatus status) {
		this.status = status;
	}

	UUID getId() {
		return this.id;
	}

	MatchPost getPost() {
		return this.post;
	}

	User getUser() {
		return this.user;
	}

	Team getTeam() {
		return this.team;
	}

	String getMessage() {
		return this.message;
	}

	JoinRequestStatus getStatus() {
		return this.status;
	}

	Instant getCreatedAt() {
		return this.createdAt;
	}

}
