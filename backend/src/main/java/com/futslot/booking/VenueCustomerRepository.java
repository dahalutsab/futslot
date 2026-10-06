package com.futslot.booking;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface VenueCustomerRepository extends JpaRepository<VenueCustomer, UUID> {

	Optional<VenueCustomer> findByVenueIdAndPhone(UUID venueId, String phone);

}
