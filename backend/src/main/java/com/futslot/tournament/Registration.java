package com.futslot.tournament;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.futslot.team.Team;

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

/** A team's entry in a tournament. Teams beyond max_teams wait in line in the order they signed up. */
@Entity
@Table(name = "tournament_registrations")
public class Registration {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tournament_id", updatable = false)
	private Tournament tournament;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "team_id", updatable = false)
	private Team team;

	@Column(nullable = false)
	private UUID registeredBy;

	private String groupName;

	private Integer seed;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RegistrationStatus status;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected Registration() {
	}

	Registration(Tournament tournament, Team team, UUID registeredBy) {
		this.tournament = tournament;
		this.team = team;
		this.registeredBy = registeredBy;
	}

	void reRegister(UUID by) {
		this.registeredBy = by;
		this.groupName = null;
		this.seed = null;
	}

	void setStatus(RegistrationStatus status) {
		this.status = status;
	}

	/** The entry fee is in. */
	public void confirm() {
		if (this.status == RegistrationStatus.PENDING_PAYMENT) {
			this.status = RegistrationStatus.CONFIRMED;
		}
	}

	void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	void setSeed(Integer seed) {
		this.seed = seed;
	}

	public UUID getId() {
		return this.id;
	}

	public Tournament getTournament() {
		return this.tournament;
	}

	public Team getTeam() {
		return this.team;
	}

	public UUID getRegisteredBy() {
		return this.registeredBy;
	}

	public String getGroupName() {
		return this.groupName;
	}

	public Integer getSeed() {
		return this.seed;
	}

	public RegistrationStatus getStatus() {
		return this.status;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

}
