import 'package:flutter/material.dart';

import '../core/api_types.dart';
import '../core/theme.dart';
import '../l10n/l10n.dart';
import 'labels.dart';

/// "Midfielder · Intermediate · Right foot · Kathmandu · 12 games".
String playerDetails(PlayerCard p) => [
      ?p.position?.label,
      ?p.skillLevel?.label,
      if (p.preferredFoot case final foot?) l.footSide(foot.label),
      ?p.city,
      l.games(p.gamesPlayed),
    ].join(' · ');

/// A player's name and what they play, for game requests.
class PlayerCardView extends StatelessWidget {
  const PlayerCardView({super.key, required this.player});

  final PlayerCard player;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        ExcludeSemantics(
          child: CircleAvatar(
            radius: 20,
            backgroundColor: c.surface2,
            child: Text(
              player.name.characters.first.toUpperCase(),
              style: TextStyle(fontWeight: FontWeight.w600, color: c.muted),
            ),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(player.name, style: const TextStyle(fontWeight: FontWeight.w500)),
              Text(playerDetails(player), style: TextStyle(fontSize: 14, color: c.muted)),
              if (player.bio case final bio?) ...[
                const SizedBox(height: 4),
                Text(bio, style: const TextStyle(fontSize: 14)),
              ],
            ],
          ),
        ),
      ],
    );
  }
}
