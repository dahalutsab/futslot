import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../core/theme.dart';
import 'booking_model.dart';

/// A state's colour, always with its icon and words (colour alone isn't enough).
class StatusChip extends StatelessWidget {
  const StatusChip(this.state, {super.key});

  final ChipState state;

  static String label(ChipState state) => switch (state) {
        ChipState.confirmed => 'Confirmed',
        ChipState.held => 'Waiting for payment',
        ChipState.verify => 'Payment being checked',
        ChipState.completed => 'Played',
        ChipState.noshow => 'No-show',
        ChipState.cancelled => 'Cancelled',
        ChipState.expired => 'Not paid in time',
      };

  static IconData icon(ChipState state) => switch (state) {
        ChipState.confirmed => LucideIcons.circleCheck,
        ChipState.held => LucideIcons.clock,
        ChipState.verify => LucideIcons.wallet,
        ChipState.completed => LucideIcons.checkCheck,
        ChipState.noshow => LucideIcons.flag,
        ChipState.cancelled => LucideIcons.circleX,
        ChipState.expired => LucideIcons.timerOff,
      };

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final (bg, fg) = switch (state) {
      ChipState.confirmed => (c.confirmed.bg, c.confirmed.fg),
      ChipState.held => (c.held.bg, c.held.fg),
      ChipState.verify => (c.verify.bg, c.verify.fg),
      ChipState.noshow => (c.noshow.bg, c.noshow.fg),
      ChipState.completed || ChipState.cancelled || ChipState.expired => (c.surface2, c.muted),
    };
    return Container(
      height: 24,
      padding: const EdgeInsets.symmetric(horizontal: 8),
      decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(999)),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon(state), size: 13, color: fg),
          const SizedBox(width: 4),
          Text(label(state), style: TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: fg, height: 1)),
        ],
      ),
    );
  }
}
