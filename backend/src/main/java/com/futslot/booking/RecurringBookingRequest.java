package com.futslot.booking;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * "Every {@code dayOfWeek} {@code startTime}-{@code endTime} from {@code startsOn}". {@code endTime}
 * defaults to one slot, {@code endsOn} to open-ended, {@code price} (per game) to the slot prices.
 */
public record RecurringBookingRequest(@NotNull UUID courtId, @NotNull DayOfWeek dayOfWeek,
		@NotNull LocalTime startTime, LocalTime endTime, @NotNull LocalDate startsOn, LocalDate endsOn,
		@NotBlank @Size(max = 100) String customerName, @NotBlank String customerPhone,
		@DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal price) {

}
