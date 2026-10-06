package com.futslot.notification;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Delivers push notifications to device tokens (Firebase Cloud Messaging in production). */
public interface PushSender {

	Result send(List<String> tokens, String title, String body, Map<String, String> data);

	/**
	 * @param delivered whether at least one device accepted the message
	 * @param invalidTokens tokens the provider says are gone (app uninstalled), to be forgotten
	 */
	record Result(boolean delivered, Set<String> invalidTokens) {

	}

}
