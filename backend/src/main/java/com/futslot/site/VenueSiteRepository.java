package com.futslot.site;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface VenueSiteRepository extends JpaRepository<VenueSite, UUID> {

	Optional<VenueSite> findBySubdomain(String subdomain);

	Optional<VenueSite> findByCustomDomain(String customDomain);

	boolean existsBySubdomainAndVenueIdNot(String subdomain, UUID venueId);

	boolean existsByCustomDomainAndVenueIdNot(String customDomain, UUID venueId);

}
