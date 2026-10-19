package com.futslot.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record SignInRequest(@NotBlank String phone, @NotBlank @Size(max = 200) String password) {

}
