package com.futslot.venue;

import jakarta.validation.constraints.Size;

/** Shown to players who must pay an advance, e.g. "eSewa / Khalti: 9800000000 (Dhuku Futsal)". Blank clears it. */
public record PaymentInstructionsRequest(@Size(max = 1000) String instructions) {

}
