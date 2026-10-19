import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../core/badges.dart';
import '../core/session.dart';
import '../core/theme.dart';
import '../l10n/l10n.dart';

/// The bell in the top bar of each tab, with how many notifications are unread. Hidden when signed out.
class NotificationBell extends ConsumerWidget {
  const NotificationBell({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (ref.watch(sessionProvider) == null) {
      return const SizedBox.shrink();
    }
    final unread = ref.watch(badgesProvider).notifications;
    final c = context.fs;
    return IconButton(
      tooltip: unread > 0 ? l.notificationsUnread(unread) : l.notifications,
      onPressed: () => context.push('/notifications'),
      // The tooltip says the count; the badge would read it again as "9+"
      icon: ExcludeSemantics(
        child: Badge(
          isLabelVisible: unread > 0,
          backgroundColor: c.danger,
          textColor: Colors.white,
          label: Text(digits(unread > 9 ? '9+' : '$unread'), style: const TextStyle(fontFeatures: tabular)),
          child: Icon(LucideIcons.bell, size: 22, color: c.muted),
        ),
      ),
    );
  }
}
