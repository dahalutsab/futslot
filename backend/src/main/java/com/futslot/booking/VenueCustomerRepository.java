package com.futslot.booking;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface VenueCustomerRepository extends JpaRepository<VenueCustomer, UUID> {

	Optional<VenueCustomer> findByVenueIdAndPhone(UUID venueId, String phone);

	/**
	 * Links every venue's customer record with this phone that has no account yet. Doesn't clear the persistence
	 * context: it runs mid sign-in, and clearing would drop the one-time code being marked as used.
	 */
	@Modifying(flushAutomatically = true)
	@Query("update VenueCustomer c set c.userId = :userId where c.phone = :phone and c.userId is null")
	int linkAccount(UUID userId, String phone);

	Optional<VenueCustomer> findByIdAndVenueId(UUID id, UUID venueId);

	/** {@code pattern} is already lower-cased and wrapped in %...%; null lists everyone. */
	@Query("""
			select c from VenueCustomer c
			where c.venueId = :venueId
			  and (:pattern is null or lower(c.name) like :pattern escape '\\' or c.phone like :pattern escape '\\')""")
	Page<VenueCustomer> search(UUID venueId, String pattern, Pageable pageable);

}
