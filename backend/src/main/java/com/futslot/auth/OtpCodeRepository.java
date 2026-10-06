package com.futslot.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

	Optional<OtpCode> findFirstByPhoneAndPurposeOrderByCreatedAtDesc(String phone, OtpPurpose purpose);

	long countByPhoneAndCreatedAtAfter(String phone, Instant after);

}
