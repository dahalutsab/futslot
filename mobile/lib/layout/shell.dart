import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../core/theme.dart';

/// The bottom bar: Explore, Bookings, Me. Each tab keeps its own back stack.
class Shell extends StatelessWidget {
  const Shell({super.key, required this.shell});

  final StatefulNavigationShell shell;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: shell,
      bottomNavigationBar: DecoratedBox(
        decoration: BoxDecoration(border: Border(top: BorderSide(color: context.fs.border))),
        child: NavigationBar(
          selectedIndex: shell.currentIndex,
          // Tapping the open tab again goes back to its first screen
          onDestinationSelected: (index) => shell.goBranch(index, initialLocation: index == shell.currentIndex),
          destinations: const [
            NavigationDestination(icon: Icon(LucideIcons.compass), label: 'Explore'),
            NavigationDestination(icon: Icon(LucideIcons.calendarDays), label: 'Bookings'),
            NavigationDestination(icon: Icon(LucideIcons.userRound), label: 'Me'),
          ],
        ),
      ),
    );
  }
}
