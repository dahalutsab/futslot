package com.futslot.auth;

import jakarta.validation.constraints.NotBlank;

record RefreshRequest(@NotBlank String refreshToken) {

}
