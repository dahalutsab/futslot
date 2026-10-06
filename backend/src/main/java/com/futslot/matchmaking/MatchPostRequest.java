package com.futslot.matchmaking;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.futslot.team.SkillLevel;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Where and when come from {@code bookingId} (one of the player's live bookings) if given, else from
 * {@code venueId} plus {@code startsAt}, else from {@code city} plus {@code startsAt}. NEED_PLAYERS needs
 * {@code playersNeeded}; FIND_OPPONENT needs a {@code teamId} the poster leads.
 */
public record MatchPostRequest(@NotNull MatchPostType type, UUID teamId, UUID bookingId, UUID venueId,
		@Size(max = 60) String city, OffsetDateTime startsAt, @Min(1) @Max(20) Integer playersNeeded,
		SkillLevel skillLevel, @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal costPerPlayer,
		@Size(max = 500) String note) {

}
