import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/theme.dart';
import '../../l10n/l10n.dart';
import '../../shared/format.dart';
import '../../shared/widgets.dart';

/// Booked slots the player is waiting for. When one frees up they're told, and whoever books first gets it.
class WaitlistScreen extends ConsumerStatefulWidget {
  const WaitlistScreen({super.key});

  @override
  ConsumerState<WaitlistScreen> createState() => _WaitlistScreenState();
}

class _WaitlistScreenState extends ConsumerState<WaitlistScreen> {
  List<WaitlistEntry>? _entries;

  String? _error;

  Api get _api => ref.read(apiProvider);

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final list = await _api.waitlist();
      if (mounted) {
        setState(() {
          _entries = list.where((entry) => entry.live).toList();
          _error = null;
        });
      }
    } catch (error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    }
  }

  Future<void> _leave(WaitlistEntry entry) async {
    try {
      await _api.leaveWaitlist(entry.id);
      if (mounted) {
        setState(() => _entries = _entries?.where((e) => e.id != entry.id).toList());
      }
    } catch (error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final entries = _entries;
    return Scaffold(
      appBar: AppBar(title: Text(l.waitlist)),
      body: ScreenBody(
        onRefresh: _load,
        children: [
          Text(
            l.waitlistIntro,
            style: TextStyle(color: c.muted),
          ),
          if (_error case final error?) ...[const SizedBox(height: 16), Notice(error)],
          const SizedBox(height: 16),
          if (entries == null)
            if (_error == null) const Skeleton(height: 128) else const SizedBox.shrink()
          else if (entries.isEmpty)
            EmptyPanel(
              title: l.notWaiting,
              body: l.notWaitingBody,
            )
          else
            for (final entry in entries)
              Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: FsCard(
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: InkWell(
                          onTap: () => context.go(Uri(
                            path: '/venues/${entry.venueSlug}',
                            queryParameters: {'date': entry.startsAt.substring(0, 10)},
                          ).toString()),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(entry.venueName, style: const TextStyle(fontWeight: FontWeight.w500)),
                              Text(
                                '${dayLabel(entry.startsAt.substring(0, 10))} · '
                                '${l.timeRange(localClock(entry.startsAt), localClock(entry.endsAt))} · '
                                '${entry.courtName}',
                                style: TextStyle(fontSize: 14, color: c.muted, fontFeatures: tabular),
                              ),
                              if (entry.status == WaitlistStatus.notified) ...[
                                const SizedBox(height: 8),
                                Text(
                                  l.itFreedUp,
                                  style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: c.confirmed.fg),
                                ),
                              ],
                            ],
                          ),
                        ),
                      ),
                      TextButton(
                        onPressed: () => _leave(entry),
                        style: TextButton.styleFrom(foregroundColor: c.muted),
                        child: Text(l.stopWaiting, style: const TextStyle(fontSize: 14)),
                      ),
                    ],
                  ),
                ),
              ),
        ],
      ),
    );
  }
}
