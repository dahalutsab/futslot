package com.futslot.booking;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A walk-in or phone booking entered at the venue. {@code endsAt} defaults to one slot; {@code price}
 * defaults to the sum of the slot prices covered and is required when the times don't line up with slots.
 */
public record OwnerBookingRequest(@NotNull UUID courtId, @NotNull OffsetDateTime startsAt, OffsetDateTime endsAt,
		@NotBlank @Size(max = 100) String customerName, @NotBlank String customerPhone, @NotNull BookingSource source,
		@DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal price, @Size(max = 500) String notes) {

}
