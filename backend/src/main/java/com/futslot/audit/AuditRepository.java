package com.futslot.audit;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuditRepository extends JpaRepository<AuditEntry, UUID> {

	Page<AuditEntry> findByVenueIdOrderByCreatedAtDesc(UUID venueId, Pageable pageable);

	Page<AuditEntry> findAllByOrderByCreatedAtDesc(Pageable pageable);

}
