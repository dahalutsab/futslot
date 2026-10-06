package com.futslot.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.futslot.auth.SmsSender;

/** Captures outgoing SMS so tests can read the OTP that was "sent". */
public class RecordingSmsSender implements SmsSender {

	private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");

	private final Map<String, String> lastMessageByPhone = new ConcurrentHashMap<>();

	@Override
	public void send(String phone, String message) {
		this.lastMessageByPhone.put(phone, message);
	}

	public String lastCodeFor(String phone) {
		Matcher matcher = CODE.matcher(this.lastMessageByPhone.get(phone));
		if (!matcher.find()) {
			throw new IllegalStateException("No code in SMS to " + phone);
		}
		return matcher.group(1);
	}

}
