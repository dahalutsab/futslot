package com.futslot.tournament;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.futslot.venue.Court;

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

/**
 * One fixture. In a knockout, the winner moves to {@code nextMatch}: the lower-numbered of its two feeder
 * matches fills the home side. A bye is a match with no away team, decided as a walkover straight away.
 */
@Entity
@Table(name = "tournament_matches")
class TournamentMatch {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tournament_id", updatable = false)
	private Tournament tournament;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private Stage stage;

	@Column(updatable = false)
	private String groupName;

	@Column(nullable = false, updatable = false)
	private int round;

	@Column(nullable = false, updatable = false)
	private int matchNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "home_registration_id")
	private Registration home;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "away_registration_id")
	private Registration away;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "court_id")
	private Court court;

	private UUID courtBlockId;

	private Instant scheduledAt;

	private Integer homeScore;

	private Integer awayScore;

	private Integer homePenalties;

	private Integer awayPenalties;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "winner_registration_id")
	private Registration winner;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "next_match_id", updatable = false)
	private TournamentMatch nextMatch;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MatchStatus status = MatchStatus.SCHEDULED;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected TournamentMatch() {
	}

	TournamentMatch(Tournament tournament, Stage stage, String groupName, int round, int matchNumber,
			Registration home, Registration away, TournamentMatch nextMatch) {
		this.tournament = tournament;
		this.stage = stage;
		this.groupName = groupName;
		this.round = round;
		this.matchNumber = matchNumber;
		this.home = home;
		this.away = away;
		this.nextMatch = nextMatch;
	}

	void schedule(Court court, Instant at, UUID courtBlockId) {
		this.court = court;
		this.scheduledAt = at;
		this.courtBlockId = courtBlockId;
	}

	void unschedule() {
		this.court = null;
		this.scheduledAt = null;
		this.courtBlockId = null;
	}

	void decide(int homeScore, int awayScore, Integer homePenalties, Integer awayPenalties, Registration winner,
			MatchStatus status) {
		this.homeScore = homeScore;
		this.awayScore = awayScore;
		this.homePenalties = homePenalties;
		this.awayPenalties = awayPenalties;
		this.winner = winner;
		this.status = status;
	}

	/** A bye: the only team goes through. */
	void bye() {
		this.winner = this.home;
		this.status = MatchStatus.WALKOVER;
	}

	void cancel() {
		this.status = MatchStatus.CANCELLED;
	}

	void setHome(Registration home) {
		this.home = home;
	}

	void setAway(Registration away) {
		this.away = away;
	}

	boolean isBye() {
		return this.stage.isKnockout() && this.away == null && this.status == MatchStatus.WALKOVER;
	}

	boolean involves(Registration registration) {
		return registration.equals(this.home) || registration.equals(this.away);
	}

	UUID getId() {
		return this.id;
	}

	Tournament getTournament() {
		return this.tournament;
	}

	Stage getStage() {
		return this.stage;
	}

	String getGroupName() {
		return this.groupName;
	}

	int getRound() {
		return this.round;
	}

	int getMatchNumber() {
		return this.matchNumber;
	}

	Registration getHome() {
		return this.home;
	}

	Registration getAway() {
		return this.away;
	}

	Court getCourt() {
		return this.court;
	}

	UUID getCourtBlockId() {
		return this.courtBlockId;
	}

	Instant getScheduledAt() {
		return this.scheduledAt;
	}

	Integer getHomeScore() {
		return this.homeScore;
	}

	Integer getAwayScore() {
		return this.awayScore;
	}

	Integer getHomePenalties() {
		return this.homePenalties;
	}

	Integer getAwayPenalties() {
		return this.awayPenalties;
	}

	Registration getWinner() {
		return this.winner;
	}

	TournamentMatch getNextMatch() {
		return this.nextMatch;
	}

	MatchStatus getStatus() {
		return this.status;
	}

}
