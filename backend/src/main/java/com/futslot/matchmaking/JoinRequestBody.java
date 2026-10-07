package com.futslot.matchmaking;

import java.util.UUID;

import jakarta.validation.constraints.Size;

/** {@code teamId} is required to answer FIND_OPPONENT (a team the player leads) and ignored otherwise. */
public record JoinRequestBody(UUID teamId, @Size(max = 300) String message) {

}
