package com.futslot.audit;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futslot.common.PageResponse;
import com.futslot.user.User;
import com.futslot.user.UserRepository;
import com.futslot.venue.VenueAccess;

@Service
class AuditService {

	private final AuditRepository entries;

	private final UserRepository users;

	private final VenueAccess access;

	AuditService(AuditRepository entries, UserRepository users, VenueAccess access) {
		this.entries = entries;
		this.users = users;
		this.access = access;
	}

	/** The owner's view of what happened at their venue. */
	@Transactional(readOnly = true)
	PageResponse<AuditView> forVenue(UUID userId, UUID venueId, Pageable pageable) {
		this.access.requireOwner(userId, venueId);
		return views(this.entries.findByVenueIdOrderByCreatedAtDesc(venueId, pageable), false);
	}

	@Transactional(readOnly = true)
	PageResponse<AuditView> forAdmin(UUID venueId, Pageable pageable) {
		return views((venueId != null) ? this.entries.findByVenueIdOrderByCreatedAtDesc(venueId, pageable)
				: this.entries.findAllByOrderByCreatedAtDesc(pageable), true);
	}

	private PageResponse<AuditView> views(Page<AuditEntry> page, boolean admin) {
		Map<UUID, String> names = this.users
			.findAllById(page.getContent().stream().map(AuditEntry::getActorId).filter(Objects::nonNull).distinct().toList())
			.stream()
			.collect(Collectors.toMap(User::getId, User::getFullName, (a, b) -> a));
		Function<AuditEntry, AuditView> view = entry -> new AuditView(entry.getId(), entry.getActorId(),
				(entry.getActorId() != null) ? names.get(entry.getActorId()) : null, entry.getVenueId(), entry.getAction(),
				entry.getEntityType(), entry.getEntityId(), entry.getChanges(),
				(admin && entry.getIpAddress() != null) ? entry.getIpAddress().getHostAddress() : null,
				entry.getCreatedAt());
		return PageResponse.of(page, view);
	}

}
