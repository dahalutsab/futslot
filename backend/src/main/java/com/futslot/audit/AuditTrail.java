package com.futslot.audit;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Records who did what. Call inside the transaction making the change. The IP address is the client's as
 * the API sees it (behind a proxy, configure server.forward-headers-strategy so it's the real one).
 */
@Component
public class AuditTrail {

	private final AuditRepository entries;

	AuditTrail(AuditRepository entries) {
		this.entries = entries;
	}

	/** {@code details} are alternating keys and values; null values are left out. */
	public void record(UUID actorId, UUID venueId, String action, String entityType, UUID entityId, Object... details) {
		Map<String, String> changes = new LinkedHashMap<>();
		for (int i = 0; i + 1 < details.length; i += 2) {
			if (details[i + 1] != null) {
				changes.put(String.valueOf(details[i]), String.valueOf(details[i + 1]));
			}
		}
		this.entries.save(new AuditEntry(actorId, venueId, action, entityType, entityId, changes, clientAddress()));
	}

	private static InetAddress clientAddress() {
		if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
			return null;
		}
		try {
			return InetAddress.getByName(attributes.getRequest().getRemoteAddr());
		}
		catch (UnknownHostException ex) {
			return null;
		}
	}

}
