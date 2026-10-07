package com.futslot.team;

public enum TeamRole {

	CAPTAIN, VICE_CAPTAIN, MEMBER;

	/** Captains and vice-captains run the team: edit it, invite, remove members, post for it. */
	boolean leads() {
		return this != MEMBER;
	}

}
