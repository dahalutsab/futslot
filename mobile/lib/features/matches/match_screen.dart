import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/theme.dart';
import '../../l10n/l10n.dart';
import '../../shared/format.dart';
import '../../shared/player_card.dart';
import '../../shared/widgets.dart';
import 'match_summary.dart';

/// One game post. Others ask to join (as a player, or for an opponent post as a team they lead); the poster
/// accepts or declines, and numbers are shared once someone's accepted.
class MatchScreen extends ConsumerStatefulWidget {
  const MatchScreen({super.key, required this.id});

  final String id;

  @override
  ConsumerState<MatchScreen> createState() => _MatchScreenState();
}

class _MatchScreenState extends ConsumerState<MatchScreen> {
  MatchPost? _post;

  /// Teams the player leads, to answer an opponent post.
  var _teams = <Team>[];

  String? _teamId;

  final _message = TextEditingController();

  var _busy = false;

  String? _error;

  Api get _api => ref.read(apiProvider);

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _message.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final post = await _api.matchPost(widget.id);
      if (mounted) {
        setState(() {
          _post = post;
          _error = null;
        });
      }
      if (post.type == MatchPostType.findOpponent && !post.mine && _teams.isEmpty) {
        final teams = (await _api.myTeams()).where((team) => team.leads).toList();
        if (mounted) {
          setState(() {
            _teams = teams;
            _teamId = teams.firstOrNull?.id;
          });
        }
      }
    } catch (error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    }
  }

  Future<void> _act(Future<MatchPost> Function(MatchPost post) action) async {
    final post = _post;
    if (post == null || _busy) {
      return;
    }
    FocusScope.of(context).unfocus();
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final updated = await action(post);
      if (mounted) {
        setState(() => _post = updated);
      }
    } catch (error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final post = _post;
    return Scaffold(
      appBar: AppBar(title: Text(l.game)),
      body: switch ((post, _error)) {
        (null, final String error) => ScreenBody(children: [ErrorPanel(message: error, onRetry: _load)]),
        (final MatchPost p, _) => ScreenBody(onRefresh: _load, children: _body(context, p)),
        _ => const ScreenBody(children: [Skeleton(height: 192)]),
      },
    );
  }

  List<Widget> _body(BuildContext context, MatchPost p) {
    final c = context.fs;
    return [
      FsCard(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            MatchSummary(post: p),
            if (p.note case final note?) ...[const SizedBox(height: 12), Text(note)],
            if (p.venue case final venue? when venue.slug != null) ...[
              const SizedBox(height: 4),
              TextButton(
                onPressed: () => context.push('/venues/${venue.slug}'),
                style: TextButton.styleFrom(padding: EdgeInsets.zero),
                child: Text(l.seeVenue(venue.name), style: const TextStyle(fontSize: 14)),
              ),
            ],
          ],
        ),
      ),
      if (_error case final error?) ...[const SizedBox(height: 16), Notice(error)],
      const SizedBox(height: 16),
      if (p.mine)
        ..._requests(context, p)
      else if (p.myRequestStatus case final status? when status != JoinRequestStatus.withdrawn)
        _yourRequest(context, p, status)
      else if (p.status == MatchPostStatus.open)
        _ask(context, p)
      else
        Text(l.noLongerTakingPlayers, style: TextStyle(color: c.muted)),
    ];
  }

  /// The poster: requests to answer.
  List<Widget> _requests(BuildContext context, MatchPost p) {
    final c = context.fs;
    return [
      SectionTitle(l.askingToJoin),
      if (p.requests.isEmpty) Text(l.noOneYet, style: TextStyle(color: c.muted)),
      for (final r in p.requests)
        Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: FsCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                PlayerCardView(player: r.player),
                if (r.team case final team? when team.slug != null) ...[
                  const SizedBox(height: 8),
                  InkWell(
                    onTap: () => context.push('/teams/${team.slug}'),
                    child: Text.rich(
                      TextSpan(
                        text: l.forTeamBefore,
                        children: [
                          TextSpan(text: team.name, style: const TextStyle(fontWeight: FontWeight.w500)),
                          TextSpan(text: l.forTeamAfter),
                        ],
                      ),
                      style: const TextStyle(fontSize: 14),
                    ),
                  ),
                ],
                if (r.message case final message?) ...[
                  const SizedBox(height: 8),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                    decoration: BoxDecoration(color: c.surface2, borderRadius: BorderRadius.circular(8)),
                    child: Text(message, style: const TextStyle(fontSize: 14)),
                  ),
                ],
                const SizedBox(height: 12),
                if (r.status == JoinRequestStatus.pending && p.status == MatchPostStatus.open)
                  Row(
                    children: [
                      FilledButton(
                        onPressed: _busy ? null : () => _act((post) => _api.answerRequest(post.id, r.id, true)),
                        child: Text(l.accept),
                      ),
                      const SizedBox(width: 8),
                      TextButton(
                        onPressed: _busy ? null : () => _act((post) => _api.answerRequest(post.id, r.id, false)),
                        style: TextButton.styleFrom(foregroundColor: c.muted),
                        child: Text(l.decline),
                      ),
                    ],
                  )
                else
                  Text(
                    [
                      switch (r.status) {
                        JoinRequestStatus.accepted => l.accepted,
                        JoinRequestStatus.declined => l.declined,
                        JoinRequestStatus.withdrawn => l.withdrew,
                        JoinRequestStatus.pending => l.waiting,
                      },
                      if (r.phone case final number?) phone(number),
                    ].join(' · '),
                    style: TextStyle(
                      fontSize: 14,
                      fontFeatures: tabular,
                      color: r.status == JoinRequestStatus.accepted ? c.confirmed.fg : c.muted,
                    ),
                  ),
              ],
            ),
          ),
        ),
      if (p.status == MatchPostStatus.open) ...[
        const SizedBox(height: 12),
        Align(
          alignment: Alignment.centerLeft,
          child: TextButton(
            onPressed: _busy ? null : () => _act((post) => _api.cancelMatchPost(post.id)),
            style: TextButton.styleFrom(foregroundColor: c.danger),
            child: Text(l.callGameOff),
          ),
        ),
      ],
    ];
  }

  /// Someone who asked: where their request stands.
  Widget _yourRequest(BuildContext context, MatchPost p, JoinRequestStatus status) {
    final c = context.fs;
    final accepted = status == JoinRequestStatus.accepted;
    return FsCard(
      padding: const EdgeInsets.all(20),
      color: accepted ? c.confirmed.bg : null,
      borderColor: accepted ? c.confirmed.line : null,
      child: switch (status) {
        JoinRequestStatus.accepted => Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(l.youreInExclaim, style: TextStyle(fontWeight: FontWeight.w600, color: c.confirmed.fg)),
              if (p.contactPhone case final number?) ...[
                const SizedBox(height: 4),
                Text(
                  l.callOrMessage(p.postedByName, phone(number)),
                  style: TextStyle(color: c.confirmed.fg, fontFeatures: tabular),
                ),
              ],
            ],
          ),
        JoinRequestStatus.declined => Text(l.pickedSomeoneElse(p.postedByName)),
        _ => Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(l.askedToJoinDecides(p.postedByName)),
              const SizedBox(height: 12),
              OutlinedButton(
                onPressed: _busy ? null : () => _act((post) => _api.withdrawRequest(post.id)),
                child: Text(l.withdraw),
              ),
            ],
          ),
      },
    );
  }

  /// Everyone else: ask to join.
  Widget _ask(BuildContext context, MatchPost p) {
    final c = context.fs;
    final opponent = p.type == MatchPostType.findOpponent;
    return FsCard(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(opponent ? l.playThemWithTeam : l.askToJoin, style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 12),
          if (opponent && _teams.isEmpty) ...[
            Text(l.needCaptainToTakeOn, style: TextStyle(color: c.muted)),
            Align(
              alignment: Alignment.centerLeft,
              child: TextButton(onPressed: () => context.go('/teams'), child: const Text('Start a team')),
            ),
          ] else ...[
            if (opponent) ...[
              FieldLabel(l.yourTeam),
              DropdownButtonFormField<String>(
                initialValue: _teamId,
                items: [for (final team in _teams) DropdownMenuItem(value: team.id, child: Text(team.name))],
                onChanged: (id) => setState(() => _teamId = id),
              ),
              const SizedBox(height: 12),
            ],
            FieldLabel(l.message, optional: true),
            TextField(
              controller: _message,
              maxLength: 300,
              textCapitalization: TextCapitalization.sentences,
              decoration: InputDecoration(hintText: l.joinMessageHint, counterText: ''),
            ),
            const SizedBox(height: 12),
            Align(
              alignment: Alignment.centerLeft,
              child: FilledButton(
                onPressed: _busy
                    ? null
                    : () => _act((post) => _api.askToJoin(
                          post.id,
                          message: _message.text.trim().isEmpty ? null : _message.text.trim(),
                          teamId: opponent ? _teamId : null,
                        )),
                child: BusyLabel(busy: _busy, label: l.askToJoin, busyLabel: l.askingEllipsis),
              ),
            ),
            const SizedBox(height: 4),
            Row(
              children: [
                Text(l.theySeeProfile, style: TextStyle(fontSize: 14, color: c.subtle)),
                TextButton(onPressed: () => context.push('/me/profile'), child: Text(l.editIt)),
              ],
            ),
          ],
        ],
      ),
    );
  }
}
