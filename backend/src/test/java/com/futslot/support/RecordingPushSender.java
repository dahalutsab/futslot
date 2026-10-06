package com.futslot.support;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.futslot.notification.PushSender;

/** Captures pushes instead of sending them. */
public class RecordingPushSender implements PushSender {

	private final List<Push> sent = new CopyOnWriteArrayList<>();

	@Override
	public Result send(List<String> tokens, String title, String body, Map<String, String> data) {
		this.sent.add(new Push(List.copyOf(tokens), title, body, Map.copyOf(data)));
		return new Result(true, Set.of());
	}

	public List<Push> sentTo(String token) {
		return this.sent.stream().filter(push -> push.tokens().contains(token)).toList();
	}

	public record Push(List<String> tokens, String title, String body, Map<String, String> data) {

	}

}
