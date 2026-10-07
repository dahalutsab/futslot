package com.futslot.site;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where venue sites live. {@code baseDomain} is the domain sites are subdomains of (futslot.com; localhost in
 * development, which browsers resolve for any subdomain), and {@code urlTemplate} turns a subdomain into the
 * site's address, e.g. {@code https://{subdomain}.futslot.com}. Custom domains point a CNAME at
 * {@code dnsTarget}. {@code sessionSecret} signs the cookie that keeps players signed in on venue sites.
 */
@ConfigurationProperties("futslot.sites")
record SiteProperties(String baseDomain, String urlTemplate, String dnsTarget, String sessionSecret) {

	String urlFor(String subdomain) {
		return this.urlTemplate.replace("{subdomain}", subdomain);
	}

}
