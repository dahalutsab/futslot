package com.futslot.venue;

import jakarta.validation.constraints.Size;

/** Any field left out stays as it is; {@code cover: true} makes this the cover photo. */
public record PhotoUpdateRequest(@Size(max = 200) String caption, Integer sortOrder, Boolean cover) {

}
