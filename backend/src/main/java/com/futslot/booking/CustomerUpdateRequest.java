package com.futslot.booking;

import jakarta.validation.constraints.Size;

/** Null fields are left unchanged; blank notes clear them. */
public record CustomerUpdateRequest(@Size(min = 1, max = 100) String name, @Size(max = 2000) String notes) {

}
