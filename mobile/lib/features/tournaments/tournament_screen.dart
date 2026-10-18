import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/session.dart';
import '../../core/theme.dart';
import '../../shared/format.dart';
import '../../shared/labels.dart';
import '../../shared/pay_form.dart';
import '../../shared/widgets.dart';
import 'tournaments_screen.dart';

/// Fixtures by stage (and group), in order, leaving out byes.
List<({String label, List<Fixture> fixtures})> tournamentRounds(List<Fixture> matches) {
  final rounds = <String, List<Fixture>>{};
  for (final match in matches.where((match) => !match.bye)) {
    rounds.putIfAbsent('${match.stage.api} ${match.groupName ?? ''}', () => []).add(match);
  }
  return [
    for (final fixtures in rounds.values)
      (
        label: [fixtures.first.stage.label, if (fixtures.first.groupName case final group?) 'Group $group'].join(' · '),
        fixtures: fixtures,
      ),
  ];
}

/// "2–1", "1–1 (4–3 pens)", or before it's played the kick-off time, or "vs".
String fixtureScore(Fixture match) {
  final (home, away) = (match.homeScore, match.awayScore);
  if (home == null || away == null) {
    return match.scheduledAt != null ? localClock(match.scheduledAt!) : 'vs';
  }
  final penalties = match.homePenalties != null ? ' (${match.homePenalties}–${match.awayPenalties} pens)' : '';
  return '$home–$away$penalties';
}

/// One tournament: what it is, entering a team (captains pick the squad), paying the entry fee to the venue, and
/// once it's on, the fixtures and tables.
class TournamentScreen extends ConsumerStatefulWidget {
  const TournamentScreen({super.key, required this.slug});

  final String slug;

  @override
  ConsumerState<TournamentScreen> createState() => _TournamentScreenState();
}

class _TournamentScreenState extends ConsumerState<TournamentScreen> {
  TournamentDetail? _tournament;

  /// The venue's own wallet or account, for the entry fee.
  String? _paymentInstructions;

  /// Entries of the player's teams in this tournament, whatever their state.
  var _mine = <TournamentEntry>[];

  /// Teams the player leads that could enter.
  var _teams = <Team>[];

  var _entering = false;

  String? _teamId;

  /// Squad picks: a shirt number field by player.
  final _squad = <String, TextEditingController>{};

  var _busy = false;

  String? _error;

  Api get _api => ref.read(apiProvider);

  bool get _signedIn => ref.read(sessionProvider) != null;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _clearSquad();
    super.dispose();
  }

  void _clearSquad() {
    for (final field in _squad.values) {
      field.dispose();
    }
    _squad.clear();
  }

  Future<void> _load() async {
    try {
      final t = await _api.tournament(widget.slug);
      if (mounted) {
        setState(() {
          _tournament = t;
          _error = null;
        });
      }
      if (t.venue.slug case final slug?) {
        _api.venue(slug).then((venue) {
          if (mounted) {
            setState(() => _paymentInstructions = venue.paymentInstructions);
          }
        }, onError: (_) {});
      }
      if (_signedIn) {
        await _loadMine(t.id);
        final teams = (await _api.myTeams()).where((team) => team.leads).toList();
        if (mounted) {
          setState(() => _teams = teams);
        }
      }
    } catch (error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    }
  }

  Future<void> _loadMine(String tournamentId) async {
    final entries = await _api.myEntries();
    if (mounted) {
      setState(() => _mine = [
            for (final e in entries)
              if (e.tournament.id == tournamentId && e.entry.status != EntryStatus.withdrawn) e.entry,
          ]);
    }
  }

  void _startEntry() {
    if (!_signedIn) {
      context.push(Uri(path: '/login', queryParameters: {'next': '/tournaments/${widget.slug}'}).toString());
      return;
    }
    setState(() {
      _entering = true;
      _pickTeam(_teams.firstOrNull?.id);
    });
  }

  void _pickTeam(String? teamId) {
    _teamId = teamId;
    _clearSquad();
  }

  void _toggle(String userId) {
    setState(() {
      _error = null;
      if (_squad.remove(userId) case final field?) {
        field.dispose();
      } else {
        _squad[userId] = TextEditingController();
      }
    });
  }

  Future<void> _act(Future<void> Function() action) async {
    if (_busy) {
      return;
    }
    FocusScope.of(context).unfocus();
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await action();
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

  Future<void> _enter(TournamentDetail t) async {
    final teamId = _teamId;
    if (teamId == null) {
      return;
    }
    if (_squad.length < t.teamSize) {
      setState(() => _error = 'Pick at least ${t.teamSize} players.');
      return;
    }
    final squad = {for (final MapEntry(:key, :value) in _squad.entries) key: int.tryParse(value.text)};
    await _act(() async {
      final updated = await _api.register(t.id, teamId, squad);
      if (mounted) {
        setState(() {
          _tournament = updated;
          _entering = false;
          _clearSquad();
        });
      }
      await _loadMine(t.id);
    });
  }

  @override
  Widget build(BuildContext context) {
    final t = _tournament;
    return Scaffold(
      appBar: AppBar(title: const Text('Tournament')),
      body: switch ((t, _error)) {
        (null, final String error) => ScreenBody(children: [ErrorPanel(message: error, onRetry: _load)]),
        (final TournamentDetail t, _) => ScreenBody(maxWidth: 768, onRefresh: _load, children: _body(context, t)),
        _ => const ScreenBody(children: [Skeleton(height: 256)]),
      },
    );
  }

  List<Widget> _body(BuildContext context, TournamentDetail t) {
    final c = context.fs;
    final text = Theme.of(context).textTheme;
    final rounds = tournamentRounds(t.matches);
    return [
      if (t.bannerUrl != null) ...[
        ClipRRect(
          borderRadius: BorderRadius.circular(12),
          child: AspectRatio(aspectRatio: 16 / 6, child: TournamentBanner(url: t.bannerUrl)),
        ),
        const SizedBox(height: 16),
      ],
      Text(t.status.label, style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: c.tournament.fg)),
      Text(t.name, style: text.headlineSmall),
      const SizedBox(height: 4),
      InkWell(
        onTap: t.venue.slug == null ? null : () => context.push('/venues/${t.venue.slug}'),
        child: Text(
          '${[t.venue.name, ?t.venue.city].join(', ')} · ${tournamentDates(t.startsOn, t.endsOn)}',
          style: TextStyle(color: c.muted, fontFeatures: tabular),
        ),
      ),
      if (t.champion case final champion?) ...[
        const SizedBox(height: 16),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          decoration: BoxDecoration(color: c.tournament.bg, borderRadius: BorderRadius.circular(12)),
          child: Row(
            children: [
              Icon(LucideIcons.trophy, size: 20, color: c.tournament.fg),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  '${champion.name} won',
                  style: TextStyle(fontWeight: FontWeight.w600, color: c.tournament.fg),
                ),
              ),
            ],
          ),
        ),
      ],
      const SizedBox(height: 16),
      GridView.count(
        crossAxisCount: 2,
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        mainAxisSpacing: 12,
        crossAxisSpacing: 12,
        childAspectRatio: 2.4,
        children: [
          _Fact('Format', t.format.label),
          _Fact('Entry', t.entryFee > 0 ? rupees(t.entryFee) : 'Free'),
          _Fact('Teams', '${t.teamsIn} of ${t.maxTeams}'),
          _Fact('Squads', '${t.teamSize}–${t.maxSquadSize} players'),
        ],
      ),
      if (t.description case final description?) ...[const SizedBox(height: 16), Text(description)],
      if (t.prizeDetails case final prizes?) ...[
        const SizedBox(height: 16),
        const SectionTitle('Prizes'),
        Text(prizes, style: TextStyle(color: c.muted)),
      ],
      if (t.rules case final rules?) ...[
        const SizedBox(height: 16),
        FsCard(
          padding: EdgeInsets.zero,
          child: ExpansionTile(
            title: const Text('Rules', style: TextStyle(fontWeight: FontWeight.w600)),
            shape: const Border(),
            collapsedShape: const Border(),
            childrenPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
            expandedAlignment: Alignment.centerLeft,
            children: [Text(rules, style: TextStyle(color: c.muted))],
          ),
        ),
      ],
      // While entering, problems show in the form, by the button
      if (_error case final error? when !_entering) ...[const SizedBox(height: 16), Notice(error)],
      for (final entry in _mine) ...[const SizedBox(height: 20), _entryCard(context, t, entry)],
      if (t.acceptingTeams) ...[const SizedBox(height: 20), ..._entry(context, t)],
      for (final table in t.tables)
        if (table.rows.isNotEmpty) ...[
          const SizedBox(height: 28),
          SectionTitle(table.groupName != null ? 'Group ${table.groupName}' : 'Table'),
          _Table(table: table),
        ],
      if (rounds.isNotEmpty) ...[
        const SizedBox(height: 28),
        const SectionTitle('Fixtures'),
        for (final round in rounds) ...[
          Padding(
            padding: const EdgeInsets.only(top: 8, bottom: 8),
            child: Text(round.label, style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: c.muted)),
          ),
          FsCard(
            padding: EdgeInsets.zero,
            child: Column(
              children: [
                for (final (i, match) in round.fixtures.indexed) ...[
                  if (i > 0) const Divider(),
                  _FixtureRow(match: match),
                ],
              ],
            ),
          ),
        ],
      ] else if (t.registrations.isNotEmpty) ...[
        const SizedBox(height: 28),
        const SectionTitle('Teams in'),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            for (final r in t.registrations)
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                decoration: BoxDecoration(color: c.surface, borderRadius: BorderRadius.circular(999)),
                child: Text(r.team.name, style: const TextStyle(fontSize: 14)),
              ),
          ],
        ),
      ],
    ];
  }

  /// One of the player's team's entries: where it stands, withdrawing, and paying the fee.
  Widget _entryCard(BuildContext context, TournamentDetail t, TournamentEntry entry) {
    final c = context.fs;
    final confirmed = entry.status == EntryStatus.confirmed;
    final open = t.status == TournamentStatus.registrationOpen || t.status == TournamentStatus.registrationClosed;
    return FsCard(
      padding: const EdgeInsets.all(20),
      color: confirmed ? c.confirmed.bg : null,
      borderColor: confirmed ? c.confirmed.line : null,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(entry.team.name, style: const TextStyle(fontWeight: FontWeight.w600)),
                    Text(
                      [entry.status.label, if (entry.groupName case final group?) 'Group $group'].join(' · '),
                      style: TextStyle(fontSize: 14, color: confirmed ? c.confirmed.fg : c.muted),
                    ),
                  ],
                ),
              ),
              if (entry.status != EntryStatus.rejected && open)
                TextButton(
                  onPressed: _busy
                      ? null
                      : () => _act(() async {
                            final updated = await _api.withdrawEntry(t.id, entry.id);
                            if (mounted) {
                              setState(() => _tournament = updated);
                            }
                            await _loadMine(t.id);
                          }),
                  style: TextButton.styleFrom(foregroundColor: c.muted),
                  child: const Text('Withdraw'),
                ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            'Squad: ${[
              for (final p in entry.squad) p.jerseyNumber != null ? '${p.name} (${p.jerseyNumber})' : p.name,
            ].join(', ')}',
            style: TextStyle(fontSize: 14, color: c.muted),
          ),
          if (entry.status == EntryStatus.pendingPayment && t.entryFee > 0) ...[
            const SizedBox(height: 16),
            Text('Pay ${rupees(t.entryFee)} to ${t.venue.name}', style: const TextStyle(fontWeight: FontWeight.w500)),
            const SizedBox(height: 8),
            PayForm(
              amount: t.entryFee,
              instructions: _paymentInstructions,
              busy: _busy,
              label: "I've paid",
              onPaid: (claim) => _act(() async {
                await _api.payEntry(entry.id, claim);
                await _load();
              }),
            ),
            const SizedBox(height: 8),
            Text('The venue checks it and confirms your place.', style: TextStyle(fontSize: 14, color: c.subtle)),
          ],
        ],
      ),
    );
  }

  /// Entering a team: pick one the player leads, then its squad and shirt numbers.
  List<Widget> _entry(BuildContext context, TournamentDetail t) {
    final c = context.fs;
    if (!_entering) {
      return [
        FilledButton(
          onPressed: _startEntry,
          style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(48)),
          child: const Text('Enter a team'),
        ),
        if (t.registrationClosesAt case final closes?) ...[
          const SizedBox(height: 8),
          Text('Entries close ${dayLabel(closes.substring(0, 10))}.', style: TextStyle(fontSize: 14, color: c.subtle)),
        ],
      ];
    }
    final team = _teams.where((team) => team.id == _teamId).firstOrNull;
    return [
      FsCard(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('Enter your team', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            if (_teams.isEmpty) ...[
              Text("Only a team's captain or vice-captain can enter it.", style: TextStyle(color: c.muted)),
              Align(
                alignment: Alignment.centerLeft,
                child: TextButton(onPressed: () => context.go('/teams'), child: const Text('Start a team')),
              ),
            ] else ...[
              const FieldLabel('Team'),
              DropdownButtonFormField<String>(
                initialValue: _teamId,
                items: [for (final team in _teams) DropdownMenuItem(value: team.id, child: Text(team.name))],
                onChanged: (id) => setState(() => _pickTeam(id)),
              ),
              if (team != null) ...[
                const SizedBox(height: 16),
                FieldLabel('Squad (${_squad.length} of ${t.teamSize}–${t.maxSquadSize})'),
                FsCard(
                  padding: EdgeInsets.zero,
                  child: Column(
                    children: [
                      for (final (i, m) in team.members.where((m) => m.status == MemberStatus.active).indexed) ...[
                        if (i > 0) const Divider(),
                        _SquadRow(
                          member: m,
                          number: _squad[m.userId],
                          full: _squad.length >= t.maxSquadSize,
                          onToggle: () => _toggle(m.userId),
                        ),
                      ],
                    ],
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  "Only active team members can play. Invite more from the team's page.",
                  style: TextStyle(fontSize: 14, color: c.subtle),
                ),
              ],
              if (_error case final error?) ...[const SizedBox(height: 16), Notice(error)],
              const SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: FilledButton(
                      onPressed: _busy ? null : () => _enter(t),
                      child: BusyLabel(
                        busy: _busy,
                        label: t.entryFee > 0 ? 'Enter, then pay ${rupees(t.entryFee)}' : 'Enter',
                        busyLabel: 'Entering…',
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  TextButton(onPressed: () => setState(() => _entering = false), child: const Text('Cancel')),
                ],
              ),
            ],
          ],
        ),
      ),
    ];
  }
}

class _Fact extends StatelessWidget {
  const _Fact(this.label, this.value);

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(color: c.surface, borderRadius: BorderRadius.circular(8)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(label, style: TextStyle(fontSize: 12, color: c.muted)),
          Text(value, style: const TextStyle(fontWeight: FontWeight.w500, fontFeatures: tabular)),
        ],
      ),
    );
  }
}

/// A team member to tick into the squad, with their shirt number once they're in.
class _SquadRow extends StatelessWidget {
  const _SquadRow({required this.member, required this.number, required this.full, required this.onToggle});

  final TeamMember member;
  final TextEditingController? number;

  /// The squad is at its most, so only picked players can be changed.
  final bool full;
  final VoidCallback onToggle;

  @override
  Widget build(BuildContext context) {
    final picked = number != null;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 4),
      child: Row(
        children: [
          Expanded(
            child: CheckboxListTile(
              value: picked,
              onChanged: picked || !full ? (_) => onToggle() : null,
              controlAffinity: ListTileControlAffinity.leading,
              contentPadding: EdgeInsets.zero,
              title: Text(member.name, overflow: TextOverflow.ellipsis),
            ),
          ),
          if (number != null)
            SizedBox(
              width: 72,
              child: Semantics(
                label: 'Shirt number for ${member.name}',
                child: TextField(
                  controller: number,
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(2)],
                  style: const TextStyle(fontFeatures: tabular),
                  decoration: const InputDecoration(hintText: 'No.'),
                ),
              ),
            ),
          const SizedBox(width: 8),
        ],
      ),
    );
  }
}

/// A league or group table: played, won, drawn, lost, goal difference, points.
class _Table extends StatelessWidget {
  const _Table({required this.table});

  final StandingsTable table;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    Widget cell(String text, {bool head = false, bool strong = false, bool left = false}) => Padding(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
          child: Text(
            text,
            textAlign: left ? TextAlign.left : TextAlign.right,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              fontSize: 14,
              color: head ? c.muted : null,
              fontWeight: strong || head ? FontWeight.w500 : null,
              fontFeatures: tabular,
            ),
          ),
        );
    return FsCard(
      padding: const EdgeInsets.symmetric(horizontal: 4),
      child: Table(
        columnWidths: const {0: FlexColumnWidth()},
        defaultColumnWidth: const IntrinsicColumnWidth(),
        children: [
          TableRow(
            decoration: BoxDecoration(border: Border(bottom: BorderSide(color: c.border))),
            children: [
              for (final (i, head) in ['Team', 'P', 'W', 'D', 'L', 'GD', 'Pts'].indexed)
                cell(head, head: true, left: i == 0),
            ],
          ),
          for (final row in table.rows)
            TableRow(
              children: [
                cell(row.team.name, strong: true, left: true),
                cell('${row.played}'),
                cell('${row.won}'),
                cell('${row.drawn}'),
                cell('${row.lost}'),
                cell('${row.goalDifference > 0 ? '+' : ''}${row.goalDifference}'),
                cell('${row.points}', strong: true),
              ],
            ),
        ],
      ),
    );
  }
}

class _FixtureRow extends StatelessWidget {
  const _FixtureRow({required this.match});

  final Fixture match;

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    TextStyle side(EntryTeam? team) => TextStyle(
          fontWeight: match.winnerRegistrationId != null && match.winnerRegistrationId == team?.registrationId
              ? FontWeight.w600
              : null,
        );
    final when = match.scheduledAt;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Row(
        children: [
          Expanded(
            child: Text(
              match.home?.name ?? 'To be decided',
              textAlign: TextAlign.right,
              overflow: TextOverflow.ellipsis,
              style: side(match.home),
            ),
          ),
          ConstrainedBox(
            constraints: const BoxConstraints(minWidth: 72),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              child: Column(
                children: [
                  Text(fixtureScore(match), style: const TextStyle(fontWeight: FontWeight.w600, fontFeatures: tabular)),
                  if (when != null && match.homeScore == null)
                    Text(dayLabel(when.substring(0, 10)), style: TextStyle(fontSize: 12, color: c.muted)),
                  if (match.status == FixtureStatus.walkover)
                    Text('Walkover', style: TextStyle(fontSize: 12, color: c.muted)),
                ],
              ),
            ),
          ),
          Expanded(
            child: Text(match.away?.name ?? 'To be decided', overflow: TextOverflow.ellipsis, style: side(match.away)),
          ),
        ],
      ),
    );
  }
}
