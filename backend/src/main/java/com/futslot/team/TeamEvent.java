package com.futslot.team;

import java.util.UUID;

/** Published after membership changes; consumed by notifications after commit. */
public record TeamEvent(Type type, UUID teamId, String teamName, String teamSlug, UUID userId, String userName,
		UUID actorUserId, String actorName) {

	public enum Type {

		/** {@code userId} was invited by {@code actorUserId}. */
		INVITED,
		/** {@code userId} accepted an invitation; leaders hear about it. */
		JOINED,
		/** {@code userId} was removed by {@code actorUserId}. */
		REMOVED,
		/** {@code userId} is now captain. */
		CAPTAIN_CHANGED

	}

}
