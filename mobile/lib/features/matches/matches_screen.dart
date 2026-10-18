import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/theme.dart';
import '../../shared/format.dart';
import '../../shared/notification_bell.dart';
import '../../shared/play_nav.dart';
import '../../shared/widgets.dart';
import 'match_summary.dart';

const _nepal = 'Asia/Kathmandu';

/// Pickup games: teams short of players, and teams looking for an opponent. Plus the player's own posts and
/// requests.
class MatchesScreen extends ConsumerStatefulWidget {
  const MatchesScreen({super.key});

  @override
  ConsumerState<MatchesScreen> createState() => _MatchesScreenState();
}

class _MatchesScreenState extends ConsumerState<MatchesScreen> {
  /// Open games, or the player's own posts and requests.
  var _mine = false;

  MatchPostType? _type;

  String? _day;

  List<MatchPost>? _posts;

  String? _error;

  final _days = [for (var i = 0; i < 7; i++) addDays(today(_nepal), i)];

  Api get _api => ref.read(apiProvider);

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final filter = (_mine, _type, _day);
    try {
      final List<MatchPost> posts;
      if (_mine) {
        final (own, asked) = await (_api.myMatchPosts(), _api.myMatchRequests()).wait;
        posts = [...own.content, ...asked.content]..sort((a, b) => a.startsAt.compareTo(b.startsAt));
      } else {
        posts = (await _api.matchPosts(type: _type, date: _day)).content;
      }
      // A newer filter may have been picked while this one loaded
      if (mounted && filter == (_mine, _type, _day)) {
        setState(() {
          _posts = posts;
          _error = null;
        });
      }
    } catch (error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    }
  }

  void _filter(void Function() change) {
    setState(() {
      change();
      _posts = null;
    });
    _load();
  }

  Future<void> _open(String path) async {
    await context.push(path);
    _load();
  }

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final posts = _posts;
    return Scaffold(
      appBar: AppBar(title: const Text('Games'), actions: const [NotificationBell()]),
      body: ScreenBody(
        maxWidth: 768,
        onRefresh: _load,
        children: [
          const PlayNav(current: '/matches'),
          const SizedBox(height: 16),
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Text(
                  'Short of players, or looking for a team to play? Post it, or join one.',
                  style: TextStyle(color: c.muted),
                ),
              ),
              const SizedBox(width: 12),
              FilledButton.icon(
                onPressed: () => _open('/matches/new'),
                icon: const Icon(LucideIcons.plus, size: 18),
                label: const Text('Post a game'),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Segmented(
            options: const [(false, 'Open games'), (true, 'Yours')],
            value: _mine,
            onChanged: (mine) => _filter(() => _mine = mine),
          ),
          if (!_mine) ...[
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (final (type, label) in [
                  (null, 'All'),
                  (MatchPostType.needPlayers, 'Players wanted'),
                  (MatchPostType.findOpponent, 'Opponent wanted'),
                ])
                  Pill(label: label, height: 36, selected: _type == type, onTap: () => _filter(() => _type = type)),
              ],
            ),
            const SizedBox(height: 8),
            SizedBox(
              height: 36,
              child: ListView(
                scrollDirection: Axis.horizontal,
                children: [
                  for (final (i, day) in [null, ..._days].indexed)
                    Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: Pill(
                        label: switch (i) { 0 => 'Any day', 1 => 'Today', 2 => 'Tomorrow', _ => dayLabel(day!) },
                        height: 36,
                        selected: _day == day,
                        onTap: () => _filter(() => _day = day),
                      ),
                    ),
                ],
              ),
            ),
          ],
          const SizedBox(height: 16),
          if (_error case final error?)
            ErrorPanel(message: error, onRetry: _load)
          else if (posts == null) ...[
            const Skeleton(height: 128),
            const SizedBox(height: 12),
            const Skeleton(height: 128),
          ] else if (posts.isEmpty)
            EmptyPanel(
              title: _mine ? "You haven't posted or joined a game" : 'No open games',
              body: 'Post one, and players nearby can ask to join.',
            )
          else
            for (final post in posts)
              Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: FsCard(
                  padding: EdgeInsets.zero,
                  child: InkWell(
                    onTap: () => _open('/matches/${post.id}'),
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          MatchSummary(post: post),
                          if (_standing(post) case final standing?) ...[
                            const SizedBox(height: 8),
                            Text(
                              standing,
                              style: TextStyle(
                                fontSize: 14,
                                fontWeight: FontWeight.w500,
                                color: post.myRequestStatus == JoinRequestStatus.accepted ? c.confirmed.fg : null,
                              ),
                            ),
                          ],
                        ],
                      ),
                    ),
                  ),
                ),
              ),
        ],
      ),
    );
  }

  /// Where the player stands with a post: theirs (and how many are asking), or their request.
  static String? _standing(MatchPost post) {
    if (post.mine) {
      final asking = post.requests.where((r) => r.status == JoinRequestStatus.pending).length;
      return asking > 0 ? 'Your post · $asking asking to join' : 'Your post';
    }
    return switch (post.myRequestStatus) {
      null => null,
      JoinRequestStatus.pending => 'You asked to join',
      JoinRequestStatus.accepted => "You're in",
      JoinRequestStatus.declined => 'Not this time',
      JoinRequestStatus.withdrawn => 'You withdrew',
    };
  }
}
