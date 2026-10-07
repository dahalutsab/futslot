package com.futslot.team;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.ApiException;
import com.futslot.common.Names;
import com.futslot.user.User;
import com.futslot.user.UserRepository;

@Service
public class PlayerService {

	private final PlayerProfileRepository profiles;

	private final UserRepository users;

	private final JdbcClient jdbc;

	PlayerService(PlayerProfileRepository profiles, UserRepository users, JdbcClient jdbc) {
		this.profiles = profiles;
		this.users = users;
		this.jdbc = jdbc;
	}

	@Transactional(readOnly = true)
	PlayerCard mine(UUID userId) {
		return card(user(userId), true);
	}

	@Transactional
	PlayerCard save(UUID userId, PlayerProfileRequest request) {
		PlayerProfile profile = this.profiles.findById(userId)
			.orElseGet(() -> this.profiles.save(new PlayerProfile(userId)));
		profile.apply(request);
		return card(user(userId), true);
	}

	/** Another player's card; private profiles still show the name and games played. */
	@Transactional(readOnly = true)
	public PlayerCard card(UUID userId) {
		return card(user(userId), false);
	}

	/**
	 * Completed bookings plus matches they were accepted into that have started; counted when shown
	 * instead of kept in player_profiles.games_played.
	 */
	long gamesPlayed(UUID userId) {
		return this.jdbc.sql("""
				SELECT (SELECT count(*) FROM bookings b JOIN venue_customers c ON c.id = b.customer_id
				        WHERE c.user_id = :userId AND b.status = 'COMPLETED')
				     + (SELECT count(*) FROM match_post_responses r JOIN match_posts p ON p.id = r.post_id
				        WHERE r.user_id = :userId AND r.status = 'ACCEPTED' AND p.status <> 'CANCELLED'
				          AND p.starts_at < now())""")
			.param("userId", userId)
			.query(Long.class)
			.single();
	}

	private PlayerCard card(User user, boolean own) {
		PlayerProfile profile = this.profiles.findById(user.getId())
			.filter(found -> own || found.isPublic())
			.orElse(null);
		String name = own ? user.getFullName() : Names.shortName(user.getFullName());
		long games = gamesPlayed(user.getId());
		if (profile == null) {
			return new PlayerCard(user.getId(), name, user.getAvatarUrl(), null, null, null, null, null, games);
		}
		return new PlayerCard(user.getId(), name, user.getAvatarUrl(), profile.getPosition(), profile.getSkillLevel(),
				profile.getPreferredFoot(), profile.getCity(), profile.getBio(), games);
	}

	private User user(UUID userId) {
		return this.users.findById(userId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PLAYER_NOT_FOUND", "Player not found"));
	}

}
