import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/api_types.dart';
import '../../core/theme.dart';
import '../../l10n/l10n.dart';
import '../../shared/format.dart';
import '../../shared/labels.dart';

/// "3 players needed", "Full", "Jhamsikhel Kings want a game".
String matchHeadline(MatchPost p) {
  if (p.type == MatchPostType.findOpponent) {
    return l.teamWantsGame(p.team?.name ?? p.postedByName);
  }
  final left = (p.playersNeeded ?? 0) - p.placesTaken;
  return left <= 0 ? l.full : l.playersNeededCount(left);
}

String matchStatusLabel(MatchPostStatus status) => switch (status) {
      MatchPostStatus.open => l.postOpen,
      MatchPostStatus.filled => l.full,
      MatchPostStatus.cancelled => l.postCalledOff,
      MatchPostStatus.expired => l.postOver,
    };

/// What a game post asks for, where and when: shared by the list and the post's screen.
class MatchSummary extends StatelessWidget {
  const MatchSummary({super.key, required this.post});

  final MatchPost post;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final p = post;
    final kind = p.type == MatchPostType.needPlayers ? c.confirmed : c.tournament;
    final muted = TextStyle(fontSize: 14, color: c.muted);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Wrap(
          spacing: 8,
          runSpacing: 4,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            _Tag(p.type == MatchPostType.needPlayers ? l.playersWanted : l.opponentWanted, colors: kind),
            if (p.status != MatchPostStatus.open) _Tag(matchStatusLabel(p.status)),
            if (p.skillLevel case final skill?) Text(skill.label, style: TextStyle(fontSize: 12, color: c.muted)),
          ],
        ),
        const SizedBox(height: 8),
        Text(matchHeadline(p), style: Theme.of(context).textTheme.titleMedium),
        Text(
          '${dayLabel(p.startsAt.substring(0, 10))} · ${localClock(p.startsAt)}',
          style: TextStyle(color: c.muted, fontFeatures: tabular),
        ),
        const SizedBox(height: 4),
        _IconLine(LucideIcons.mapPin, p.venue?.name ?? p.city ?? l.placeToBeAgreed, style: muted),
        const SizedBox(height: 2),
        _IconLine(
          LucideIcons.users,
          [
            p.team?.name ?? p.postedByName,
            if (p.costPerPlayer case final cost? when cost > 0) l.priceEach(rupees(cost)),
          ]
              .join(' · '),
          style: muted.copyWith(fontFeatures: tabular),
        ),
      ],
    );
  }
}

class _Tag extends StatelessWidget {
  const _Tag(this.text, {this.colors});

  final String text;
  final StateColors? colors;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(color: colors?.bg ?? c.surface2, borderRadius: BorderRadius.circular(999)),
      child: Text(text, style: TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: colors?.fg ?? c.muted)),
    );
  }
}

class _IconLine extends StatelessWidget {
  const _IconLine(this.icon, this.text, {required this.style});

  final IconData icon;
  final String text;
  final TextStyle style;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(padding: const EdgeInsets.only(top: 3), child: Icon(icon, size: 14, color: style.color)),
        const SizedBox(width: 4),
        Expanded(child: Text(text, style: style)),
      ],
    );
  }
}
