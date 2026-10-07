package com.futslot.tournament;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code groupCount} (2, 4 or 8) is for GROUP_KNOCKOUT only. Registration dates are optional; without
 * them registration runs from opening it until closing it.
 */
public record TournamentRequest(@NotBlank @Size(max = 100) String name, @Size(max = 2000) String description,
		@Size(max = 4000) String rules, @Size(max = 500) String bannerUrl, @NotNull TournamentFormat format,
		@NotNull @Min(3) @Max(11) Integer teamSize, @NotNull @Min(3) @Max(20) Integer maxSquadSize,
		@NotNull @Min(2) @Max(32) Integer maxTeams,
		@DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal entryFee, @Size(max = 500) String prizeDetails,
		@Min(0) @Max(10) Integer pointsForWin, @Min(0) @Max(10) Integer pointsForDraw,
		OffsetDateTime registrationOpensAt, OffsetDateTime registrationClosesAt, @NotNull LocalDate startsOn,
		@NotNull LocalDate endsOn, @Min(10) @Max(240) Integer matchMinutes, Integer groupCount) {

}
