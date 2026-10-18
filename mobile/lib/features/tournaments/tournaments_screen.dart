import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/config.dart';
import '../../core/session.dart';
import '../../core/theme.dart';
import '../../shared/format.dart';
import '../../shared/labels.dart';
import '../../shared/notification_bell.dart';
import '../../shared/play_nav.dart';
import '../../shared/widgets.dart';

enum _Tab { upcoming, past, mine }

/// "Sat 1 Nov – Sun 2 Nov", or one day.
String tournamentDates(String startsOn, String endsOn) =>
    startsOn == endsOn ? dayLabel(startsOn) : '${dayLabel(startsOn)} – ${dayLabel(endsOn)}';

/// Tournaments venues run: coming up, finished, and the ones the player's teams entered.
class TournamentsScreen extends ConsumerStatefulWidget {
  const TournamentsScreen({super.key});

  @override
  ConsumerState<TournamentsScreen> createState() => _TournamentsScreenState();
}

class _TournamentsScreenState extends ConsumerState<TournamentsScreen> {
  var _tab = _Tab.upcoming;

  List<TournamentSummary>? _list;

  List<MyTournamentEntry>? _entries;

  String? _error;

  Api get _api => ref.read(apiProvider);

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final tab = _tab;
    try {
      if (tab == _Tab.mine) {
        final entries = await _api.myEntries();
        if (mounted && tab == _tab) {
          setState(() {
            _entries = entries;
            _error = null;
          });
        }
      } else {
        final list = await _api.tournaments(upcoming: tab == _Tab.upcoming);
        if (mounted && tab == _tab) {
          setState(() {
            _list = list;
            _error = null;
          });
        }
      }
    } catch (error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    }
  }

  void _show(_Tab tab) {
    setState(() {
      _tab = tab;
      _list = null;
      _error = null;
    });
    _load();
  }

  Future<void> _open(String slug) async {
    await context.push('/tournaments/$slug');
    _load();
  }

  @override
  Widget build(BuildContext context) {
    final signedIn = ref.watch(sessionProvider) != null;
    return Scaffold(
      appBar: AppBar(title: const Text('Tournaments'), actions: const [NotificationBell()]),
      body: ScreenBody(
        maxWidth: 768,
        onRefresh: _load,
        children: [
          if (signedIn) ...[const PlayNav(current: '/tournaments'), const SizedBox(height: 16)],
          Segmented(
            options: [
              (_Tab.upcoming, 'Coming up'),
              (_Tab.past, 'Finished'),
              if (signedIn) (_Tab.mine, 'Your teams'),
            ],
            value: _tab,
            onChanged: _show,
          ),
          const SizedBox(height: 16),
          if (_error case final error?)
            ErrorPanel(message: error, onRetry: _load)
          else if (_tab == _Tab.mine)
            ..._mine(context)
          else if (_list case final list?)
            if (list.isEmpty)
              EmptyPanel(
                title: _tab == _Tab.upcoming ? 'No tournaments coming up' : 'No finished tournaments yet',
                body: _tab == _Tab.upcoming ? 'Check back soon.' : 'Finished tournaments and their winners show here.',
              )
            else
              for (final t in list)
                Padding(
                  padding: const EdgeInsets.only(bottom: 16),
                  child: _TournamentCard(t, onTap: () => _open(t.slug)),
                )
          else
            const Skeleton(height: 192),
        ],
      ),
    );
  }

  List<Widget> _mine(BuildContext context) {
    final c = context.fs;
    final entries = _entries;
    if (entries == null) {
      return const [Skeleton(height: 96)];
    }
    if (entries.isEmpty) {
      return const [
        EmptyPanel(
          title: "Your teams haven't entered a tournament yet",
          body: "A team's captain or vice-captain enters it from the tournament's page.",
        ),
      ];
    }
    return [
      for (final e in entries)
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: FsCard(
            padding: EdgeInsets.zero,
            child: InkWell(
              onTap: () => _open(e.tournament.slug),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(e.tournament.name, style: const TextStyle(fontWeight: FontWeight.w600)),
                    Text(
                      '${e.entry.team.name} · ${dayLabel(e.tournament.startsOn)}',
                      style: TextStyle(fontSize: 14, color: c.muted),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      e.entry.status.label,
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                        color: switch (e.entry.status) {
                          EntryStatus.pendingPayment => c.held.fg,
                          EntryStatus.confirmed => c.confirmed.fg,
                          _ => c.muted,
                        },
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
    ];
  }
}

class _TournamentCard extends StatelessWidget {
  const _TournamentCard(this.t, {required this.onTap});

  final TournamentSummary t;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    return FsCard(
      padding: EdgeInsets.zero,
      child: InkWell(
        onTap: onTap,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            AspectRatio(aspectRatio: 16 / 7, child: TournamentBanner(url: t.bannerUrl)),
            Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    t.status.label,
                    style: TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: c.tournament.fg),
                  ),
                  const SizedBox(height: 4),
                  Text(t.name, style: const TextStyle(fontWeight: FontWeight.w600)),
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      Icon(LucideIcons.mapPin, size: 14, color: c.muted),
                      const SizedBox(width: 4),
                      Expanded(
                        child: Text(
                          [t.venue.name, ?t.venue.city].join(', '),
                          style: TextStyle(fontSize: 14, color: c.muted),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '${tournamentDates(t.startsOn, t.endsOn)} · ${t.format.label}',
                    style: const TextStyle(fontSize: 14, fontFeatures: tabular),
                  ),
                  Text(
                    [
                      t.entryFee > 0 ? '${rupees(t.entryFee)} a team' : 'Free to enter',
                      '${t.teamsIn}/${t.maxTeams} teams',
                    ].join(' · '),
                    style: TextStyle(fontSize: 14, color: c.muted, fontFeatures: tabular),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// A tournament's banner, or a trophy on the tournament blue when it has none.
class TournamentBanner extends StatelessWidget {
  const TournamentBanner({super.key, required this.url});

  final String? url;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final trophy = Center(child: Icon(LucideIcons.trophy, size: 32, color: c.tournament.fg));
    return ColoredBox(
      color: c.tournament.bg,
      child: url == null
          ? trophy
          : Image.network(mediaUrl(url!), fit: BoxFit.cover, semanticLabel: '', errorBuilder: (_, _, _) => trophy),
    );
  }
}
