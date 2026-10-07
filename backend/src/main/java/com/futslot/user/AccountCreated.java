package com.futslot.user;

import java.util.UUID;

/**
 * A new FutSlot account, by signing up or being added as staff. Published inside the creating transaction so
 * other parts can attach what already exists for that phone number.
 */
public record AccountCreated(UUID userId, String phone) {

}
