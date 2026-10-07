package com.futslot.tournament;

import java.io.Serializable;
import java.util.UUID;

import com.futslot.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A player a team named for the tournament. */
@Entity
@Table(name = "tournament_squad_members")
@IdClass(SquadMember.Key.class)
class SquadMember {

	@Id
	@Column(name = "registration_id")
	private UUID registrationId;

	@Id
	@Column(name = "user_id")
	private UUID userId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", insertable = false, updatable = false)
	private User user;

	private Integer jerseyNumber;

	protected SquadMember() {
	}

	SquadMember(UUID registrationId, User user, Integer jerseyNumber) {
		this.registrationId = registrationId;
		this.userId = user.getId();
		this.user = user;
		this.jerseyNumber = jerseyNumber;
	}

	UUID getRegistrationId() {
		return this.registrationId;
	}

	UUID getUserId() {
		return this.userId;
	}

	User getUser() {
		return this.user;
	}

	Integer getJerseyNumber() {
		return this.jerseyNumber;
	}

	record Key(UUID registrationId, UUID userId) implements Serializable {

	}

}
