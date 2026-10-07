package com.futslot.split;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface BookingSplitRepository extends JpaRepository<BookingSplit, UUID> {

}
