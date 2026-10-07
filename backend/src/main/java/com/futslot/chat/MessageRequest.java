package com.futslot.chat;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code bookingId} optionally ties the message to one of the player's bookings at the venue. */
public record MessageRequest(@NotBlank @Size(max = 2000) String body, UUID bookingId) {

}
