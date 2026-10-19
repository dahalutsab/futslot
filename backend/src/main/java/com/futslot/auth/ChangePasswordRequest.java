package com.futslot.auth;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code currentPassword} is needed once the account has a password. */
record ChangePasswordRequest(@Size(max = 200) String currentPassword, @NotNull @Size(max = 200) String newPassword) {

}
