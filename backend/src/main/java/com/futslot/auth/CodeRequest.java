package com.futslot.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

record CodeRequest(@NotBlank String phone, @NotNull CodePurpose purpose) {

}
