package com.futslot.tournament;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

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
import jakarta.persistence.Table;

/** A competition a venue runs: teams register, fixtures are drawn, results decide the winner. */
@Entity
@Table(name = "tournaments")
public class Tournament {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "venue_id", updatable = false)
	private Venue venue;

	@Column(nullable = false, updatable = false)
	private UUID organizerId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true, updatable = false)
	private String slug;

	private String description;

	private String rules;

	private String bannerUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TournamentFormat format;

	@Column(nullable = false)
	private int teamSize;

	@Column(nullable = false)
	private int maxSquadSize;

	@Column(nullable = false)
	private int maxTeams;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal entryFee = BigDecimal.ZERO;

	private String prizeDetails;

	@Column(nullable = false)
	private int pointsForWin = 3;

	@Column(nullable = false)
	private int pointsForDraw = 1;

	private Instant registrationOpensAt;

	private Instant registrationClosesAt;

	@Column(nullable = false)
	private LocalDate startsOn;

	@Column(nullable = false)
	private LocalDate endsOn;

	@Column(nullable = false)
	private int matchMinutes = 60;

	private Integer groupCount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TournamentStatus status = TournamentStatus.DRAFT;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected Tournament() {
	}

	Tournament(Venue venue, UUID organizerId, String slug, TournamentRequest request) {
		this.venue = venue;
		this.organizerId = organizerId;
		this.slug = slug;
		apply(request);
	}

	void apply(TournamentRequest request) {
		this.name = request.name().strip();
		this.description = text(request.description());
		this.rules = text(request.rules());
		this.bannerUrl = text(request.bannerUrl());
		this.format = request.format();
		this.teamSize = request.teamSize();
		this.maxSquadSize = request.maxSquadSize();
		this.maxTeams = request.maxTeams();
		this.entryFee = (request.entryFee() != null) ? request.entryFee() : BigDecimal.ZERO;
		this.prizeDetails = text(request.prizeDetails());
		this.pointsForWin = (request.pointsForWin() != null) ? request.pointsForWin() : 3;
		this.pointsForDraw = (request.pointsForDraw() != null) ? request.pointsForDraw() : 1;
		this.registrationOpensAt = (request.registrationOpensAt() != null) ? request.registrationOpensAt().toInstant()
				: null;
		this.registrationClosesAt = (request.registrationClosesAt() != null)
				? request.registrationClosesAt().toInstant() : null;
		this.startsOn = request.startsOn();
		this.endsOn = request.endsOn();
		this.matchMinutes = (request.matchMinutes() != null) ? request.matchMinutes() : 60;
		this.groupCount = (request.format() == TournamentFormat.GROUP_KNOCKOUT) ? request.groupCount() : null;
	}

	void setStatus(TournamentStatus status) {
		this.status = status;
	}

	/** Teams can sign up: registration is open and, if dates were set, within them. */
	boolean acceptsRegistrations(Instant now) {
		return this.status == TournamentStatus.REGISTRATION_OPEN
				&& (this.registrationOpensAt == null || !now.isBefore(this.registrationOpensAt))
				&& (this.registrationClosesAt == null || now.isBefore(this.registrationClosesAt));
	}

	boolean fixturesDrawn() {
		return this.status == TournamentStatus.IN_PROGRESS || this.status == TournamentStatus.COMPLETED;
	}

	private static String text(String value) {
		return (value != null && !value.isBlank()) ? value.strip() : null;
	}

	public UUID getId() {
		return this.id;
	}

	public Venue getVenue() {
		return this.venue;
	}

	public String getName() {
		return this.name;
	}

	public String getSlug() {
		return this.slug;
	}

	public String getDescription() {
		return this.description;
	}

	public String getRules() {
		return this.rules;
	}

	public String getBannerUrl() {
		return this.bannerUrl;
	}

	public TournamentFormat getFormat() {
		return this.format;
	}

	public int getTeamSize() {
		return this.teamSize;
	}

	public int getMaxSquadSize() {
		return this.maxSquadSize;
	}

	public int getMaxTeams() {
		return this.maxTeams;
	}

	public BigDecimal getEntryFee() {
		return this.entryFee;
	}

	public String getPrizeDetails() {
		return this.prizeDetails;
	}

	public int getPointsForWin() {
		return this.pointsForWin;
	}

	public int getPointsForDraw() {
		return this.pointsForDraw;
	}

	public Instant getRegistrationOpensAt() {
		return this.registrationOpensAt;
	}

	public Instant getRegistrationClosesAt() {
		return this.registrationClosesAt;
	}

	public LocalDate getStartsOn() {
		return this.startsOn;
	}

	public LocalDate getEndsOn() {
		return this.endsOn;
	}

	public int getMatchMinutes() {
		return this.matchMinutes;
	}

	public Integer getGroupCount() {
		return this.groupCount;
	}

	public TournamentStatus getStatus() {
		return this.status;
	}

}
