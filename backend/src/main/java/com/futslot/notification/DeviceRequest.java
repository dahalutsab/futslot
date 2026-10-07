package com.futslot.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record DeviceRequest(@NotBlank @Size(max = 4096) String token, @NotNull Platform platform) {

}
