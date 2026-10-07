package com.futslot.site;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Empty {@code customDomain}, {@code accentColor} or {@code headline} clears them. */
record SiteRequest(@NotNull SiteTemplate template, @NotBlank @Size(max = 40) String subdomain,
		@Size(max = 253) String customDomain, @Size(max = 7) String accentColor, @Size(max = 120) String headline) {

}
