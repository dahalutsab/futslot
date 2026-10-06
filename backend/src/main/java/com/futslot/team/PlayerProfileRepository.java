package com.futslot.team;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface PlayerProfileRepository extends JpaRepository<PlayerProfile, UUID> {

}
