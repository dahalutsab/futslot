import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../l10n/l10n.dart';
import 'widgets.dart';

/// Switches between the parts of the Play tab: pickup games, tournaments and teams.
class PlayNav extends StatelessWidget {
  const PlayNav({super.key, required this.current});

  /// '/matches', '/tournaments' or '/teams'.
  final String current;

  @override
  Widget build(BuildContext context) {
    return Segmented(
      options: [('/matches', l.playGames), ('/tournaments', l.playTournaments), ('/teams', l.playTeams)],
      value: current,
      onChanged: (path) => path == current ? null : context.go(path),
    );
  }
}
