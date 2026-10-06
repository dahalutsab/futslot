package com.futslot.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByPhone(String phone);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

}
