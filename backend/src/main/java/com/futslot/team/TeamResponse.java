package com.futslot.team;

import java.util.List;
import java.util.UUID;

/**
 * Members see full names, pending invites (leaders only) and their own role; everyone else sees the
 * active squad by short name. {@code myRole} and {@code myStatus} are null for non-members.
 */
public record TeamResponse(UUID id, String name, String slug, String logoUrl, String city, SkillLevel skillLevel,
		boolean isPublic, UUID captainId, int memberCount, List<Member> members, TeamRole myRole,
		MemberStatus myStatus) {

	public record Member(UUID userId, String name, String avatarUrl, TeamRole role, MemberStatus status) {

	}

}
