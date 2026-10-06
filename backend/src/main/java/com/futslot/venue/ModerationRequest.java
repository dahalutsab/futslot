package com.futslot.venue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModerationRequest(@NotBlank @Size(max = 500) String reason) {

}
