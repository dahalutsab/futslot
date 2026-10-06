package com.futslot.discount;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

interface DiscountCodeRepository extends JpaRepository<DiscountCode, UUID> {

	boolean existsByVenueIdAndCode(UUID venueId, String code);

	Optional<DiscountCode> findByVenueIdAndCode(UUID venueId, String code);

	Optional<DiscountCode> findByIdAndVenueId(UUID id, UUID venueId);

	List<DiscountCode> findByVenueIdOrderByCreatedAtDesc(UUID venueId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select d from DiscountCode d where d.venueId = :venueId and d.code = :code")
	Optional<DiscountCode> lockByVenueIdAndCode(UUID venueId, String code);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select d from DiscountCode d where d.id = :id")
	DiscountCode lockById(UUID id);

}
