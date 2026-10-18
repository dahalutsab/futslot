import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../core/badges.dart';
import '../core/theme.dart';

/// The bottom bar: Explore, Bookings, Messages, Me. Each tab keeps its own back stack.
class Shell extends ConsumerWidget {
  const Shell({super.key, required this.shell});

  final StatefulNavigationShell shell;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final unread = ref.watch(badgesProvider).messages;
    return Scaffold(
      body: shell,
      bottomNavigationBar: DecoratedBox(
        decoration: BoxDecoration(border: Border(top: BorderSide(color: context.fs.border))),
        child: NavigationBar(
          selectedIndex: shell.currentIndex,
          // Tapping the open tab again goes back to its first screen
          onDestinationSelected: (index) => shell.goBranch(index, initialLocation: index == shell.currentIndex),
          destinations: [
            const NavigationDestination(icon: Icon(LucideIcons.compass), label: 'Explore'),
            const NavigationDestination(icon: Icon(LucideIcons.calendarDays), label: 'Bookings'),
            NavigationDestination(
              // The tooltip says the count, so the badge isn't read out as well
              icon: ExcludeSemantics(
                child: Badge(
                  isLabelVisible: unread > 0,
                  backgroundColor: context.fs.ink,
                  textColor: context.fs.inkFg,
                  label: Text('$unread', style: const TextStyle(fontFeatures: tabular)),
                  child: const Icon(LucideIcons.messageSquare),
                ),
              ),
              label: 'Messages',
              tooltip: unread > 0 ? 'Messages, $unread unread' : 'Messages',
            ),
            const NavigationDestination(icon: Icon(LucideIcons.userRound), label: 'Me'),
          ],
        ),
      ),
    );
  }
}
