import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'api.dart';
import 'session.dart';

/// Unread notifications and messages, for the counts on the bell and the Messages tab.
typedef BadgeCounts = ({int notifications, int messages});

const _none = (notifications: 0, messages: 0);

const _every = Duration(minutes: 1);

final badgesProvider = NotifierProvider<Badges, BadgeCounts>(Badges.new);

/// Checked when the player signs in, every minute after, and when the app comes back to the front. Screens that
/// read something call [refresh] so the counts drop straight away.
class Badges extends Notifier<BadgeCounts> {
  @override
  BadgeCounts build() {
    final signedIn = ref.watch(sessionProvider.select((session) => session != null));
    if (!signedIn) {
      return _none;
    }
    final timer = Timer.periodic(_every, (_) => refresh());
    ref.onDispose(timer.cancel);
    Future.microtask(refresh);
    return _none;
  }

  Future<void> refresh() async {
    if (ref.read(sessionProvider) == null) {
      return;
    }
    try {
      final api = ref.read(apiProvider);
      final (notifications, conversations) = await (api.unreadNotifications(), api.conversations()).wait;
      state = (
        notifications: notifications,
        messages: conversations.fold(0, (sum, conversation) => sum + conversation.unread),
      );
    } catch (_) {
      // Counts catch up on the next check
    }
  }
}
