package com.futslot.matchmaking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.futslot.common.Timestamps;
import com.futslot.team.SkillLevel;
import com.futslot.team.Team;
import com.futslot.user.User;
import com.futslot.venue.Venue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** A public call for players or for an opposing team at a time and place. */
@Entity
@Table(name = "match_posts")
class MatchPost {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private MatchPostType type;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by", updatable = false)
	private User createdBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "team_id", updatable = false)
	private Team team;

	@Column(updatable = false)
	private UUID bookingId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "venue_id", updatable = false)
	private Venue venue;

	@Column(nullable = false)
	private String city;

	@Column(nullable = false)
	private Instant startsAt;

	private Integer playersNeeded;

	@Enumerated(EnumType.STRING)
	private SkillLevel skillLevel;

	@Column(precision = 12, scale = 2)
	private BigDecimal costPerPlayer;

	private String note;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MatchPostStatus status = MatchPostStatus.OPEN;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected MatchPost() {
	}

	MatchPost(MatchPostType type, User createdBy, Team team, UUID bookingId, Venue venue, String city,
			Instant startsAt, Integer playersNeeded, SkillLevel skillLevel, BigDecimal costPerPlayer, String note) {
		this.type = type;
		this.createdBy = createdBy;
		this.team = team;
		this.bookingId = bookingId;
		this.venue = venue;
		this.city = city;
		this.startsAt = startsAt;
		this.playersNeeded = playersNeeded;
		this.skillLevel = skillLevel;
		this.costPerPlayer = costPerPlayer;
		this.note = note;
	}

	/** Places taken: players for NEED_PLAYERS, the one opponent for FIND_OPPONENT. */
	int capacity() {
		return (this.type == MatchPostType.NEED_PLAYERS) ? this.playersNeeded : 1;
	}

	void filled() {
		this.status = MatchPostStatus.FILLED;
	}

	/** A place came free again before kick-off. */
	void reopen(Instant now) {
		if (this.status == MatchPostStatus.FILLED && now.isBefore(this.startsAt)) {
			this.status = MatchPostStatus.OPEN;
		}
	}

	void cancel() {
		this.status = MatchPostStatus.CANCELLED;
	}

	/** OPEN posts whose time has passed read as EXPIRED before the job catches up. */
	MatchPostStatus statusAt(Instant now) {
		return (this.status == MatchPostStatus.OPEN && !now.isBefore(this.startsAt)) ? MatchPostStatus.EXPIRED
				: this.status;
	}

	boolean isOpenAt(Instant now) {
		return statusAt(now) == MatchPostStatus.OPEN;
	}

	UUID getId() {
		return this.id;
	}

	MatchPostType getType() {
		return this.type;
	}

	User getCreatedBy() {
		return this.createdBy;
	}

	Team getTeam() {
		return this.team;
	}

	UUID getBookingId() {
		return this.bookingId;
	}

	Venue getVenue() {
		return this.venue;
	}

	String getCity() {
		return this.city;
	}

	Instant getStartsAt() {
		return this.startsAt;
	}

	Integer getPlayersNeeded() {
		return this.playersNeeded;
	}

	SkillLevel getSkillLevel() {
		return this.skillLevel;
	}

	BigDecimal getCostPerPlayer() {
		return this.costPerPlayer;
	}

	String getNote() {
		return this.note;
	}

	MatchPostStatus getStatus() {
		return this.status;
	}

	Instant getCreatedAt() {
		return this.createdAt;
	}

	@PrePersist
	void stampCreated() {
		if (this.createdAt == null) {
			this.createdAt = Timestamps.now();
		}
	}

}
