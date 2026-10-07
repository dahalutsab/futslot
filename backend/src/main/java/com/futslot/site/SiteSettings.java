package com.futslot.site;

import java.util.UUID;

/**
 * A venue's website settings as the dashboard shows them. Before the owner saves anything, {@code saved} is
 * false and the fields are suggestions. {@code url} is the site's address on FutSlot, {@code previewPath} renders
 * it with any template even before publishing, and {@code live} says whether the venue is approved (only
 * approved venues can publish).
 */
public record SiteSettings(UUID venueId, boolean saved, SiteTemplate template, String subdomain, String customDomain,
		String accentColor, String headline, boolean published, String url, String previewPath, boolean live,
		String dnsTarget) {

}
