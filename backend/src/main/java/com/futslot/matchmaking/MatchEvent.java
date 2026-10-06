package com.futslot.matchmaking;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Published after matchmaking activity; {@code recipients} are who should hear about it. */
public record MatchEvent(Type type, UUID postId, MatchPostType postType, List<UUID> recipients, String actorName,
		String place, OffsetDateTime startsAt) {

	public enum Type {

		/** Someone asked to join; the poster hears. */
		REQUESTED,
		/** The poster accepted; the requester hears. */
		ACCEPTED,
		/** The poster declined, or picked someone else; the requester hears. */
		DECLINED,
		/** An accepted player pulled out; the poster hears. */
		WITHDRAWN,
		/** The poster called it off; everyone who asked hears. */
		CANCELLED

	}

}
