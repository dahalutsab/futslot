package com.futslot.auth;

import jakarta.validation.constraints.NotBlank;

record OtpRequest(@NotBlank String phone) {

}
