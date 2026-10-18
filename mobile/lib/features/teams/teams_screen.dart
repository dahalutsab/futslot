import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/theme.dart';
import '../../shared/labels.dart';
import '../../shared/notification_bell.dart';
import '../../shared/play_nav.dart';
import '../../shared/widgets.dart';

/// The player's teams and invitations, starting a team, and finding public teams.
class TeamsScreen extends ConsumerStatefulWidget {
  const TeamsScreen({super.key});

  @override
  ConsumerState<TeamsScreen> createState() => _TeamsScreenState();
}

class _TeamsScreenState extends ConsumerState<TeamsScreen> {
  List<Team>? _teams;

  List<TeamSummary>? _found;

  final _q = TextEditingController();

  var _creating = false;

  final _name = TextEditingController();

  final _city = TextEditingController();

  SkillLevel? _skill;

  var _busy = false;

  String? _error;

  Api get _api => ref.read(apiProvider);

  @override
  void initState() {
    super.initState();
    _load();
    _search();
  }

  @override
  void dispose() {
    for (final field in [_q, _name, _city]) {
      field.dispose();
    }
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final teams = await _api.myTeams();
      if (mounted) {
        setState(() {
          _teams = teams;
          _error = null;
        });
      }
    } catch (error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    }
  }

  Future<void> _search() async {
    try {
      final q = _q.text.trim();
      final found = await _api.teams(q: q.isEmpty ? null : q);
      if (mounted) {
        setState(() => _found = found.content);
      }
    } catch (error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    }
  }

  Future<void> _open(String slug) async {
    await context.push('/teams/$slug');
    _load();
  }

  Future<void> _create() async {
    final name = _name.text.trim();
    if (name.length < 2) {
      setState(() => _error = 'Give the team a name.');
      return;
    }
    FocusScope.of(context).unfocus();
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final city = _city.text.trim();
      final team = await _api.createTeam(name: name, city: city.isEmpty ? null : city, skillLevel: _skill);
      if (mounted) {
        setState(() {
          _creating = false;
          _name.clear();
          _city.clear();
          _skill = null;
        });
        _open(team.slug);
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

  Future<void> _answer(Team team, bool accept) async {
    try {
      await _api.answerInvite(team.id, accept);
      await _load();
    } catch (error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final teams = _teams;
    final invites = teams?.where((team) => team.myStatus == MemberStatus.invited).toList() ?? const [];
    final joined = teams?.where((team) => team.joined).toList() ?? const [];
    final found = _found;
    return Scaffold(
      appBar: AppBar(title: const Text('Teams'), actions: const [NotificationBell()]),
      body: ScreenBody(
        maxWidth: 768,
        onRefresh: () async {
          await Future.wait([_load(), _search()]);
        },
        children: [
          const PlayNav(current: '/teams'),
          if (_error case final error?) ...[const SizedBox(height: 16), Notice(error)],
          if (invites.isNotEmpty) ...[
            const SizedBox(height: 24),
            const SectionTitle('Invitations'),
            for (final team in invites)
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: FsCard(
                  borderColor: c.held.line,
                  child: Row(
                    children: [
                      Expanded(
                        child: InkWell(
                          onTap: () => _open(team.slug),
                          child: Text(team.name, style: const TextStyle(fontWeight: FontWeight.w500)),
                        ),
                      ),
                      FilledButton(onPressed: () => _answer(team, true), child: const Text('Join')),
                      TextButton(
                        onPressed: () => _answer(team, false),
                        style: TextButton.styleFrom(foregroundColor: c.muted),
                        child: const Text('Decline'),
                      ),
                    ],
                  ),
                ),
              ),
          ],
          const SizedBox(height: 24),
          SectionTitle(
            'Your teams',
            action: OutlinedButton.icon(
              onPressed: () => setState(() => _creating = !_creating),
              icon: const Icon(LucideIcons.plus, size: 16),
              label: const Text('New team'),
            ),
          ),
          if (_creating) ...[
            FsCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const FieldLabel('Name'),
                  TextField(
                    controller: _name,
                    maxLength: 60,
                    textCapitalization: TextCapitalization.words,
                    decoration: const InputDecoration(counterText: ''),
                  ),
                  const SizedBox(height: 12),
                  const FieldLabel('City'),
                  TextField(
                    controller: _city,
                    maxLength: 60,
                    textCapitalization: TextCapitalization.words,
                    decoration: const InputDecoration(counterText: ''),
                  ),
                  const SizedBox(height: 12),
                  PillChoices(
                    label: 'Level',
                    options: [for (final s in SkillLevel.values) (s, s.label)],
                    value: _skill,
                    onChanged: (s) => setState(() => _skill = s),
                  ),
                  Align(
                    alignment: Alignment.centerLeft,
                    child: FilledButton(
                      onPressed: _busy ? null : _create,
                      child: BusyLabel(busy: _busy, label: 'Create team', busyLabel: 'Creating…'),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    "You'll be the captain, and can invite players by phone.",
                    style: TextStyle(fontSize: 14, color: c.subtle),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 12),
          ],
          if (teams == null && _error == null)
            const Skeleton(height: 72)
          else if (joined.isEmpty && !_creating)
            Text(
              "You're not in a team yet. Start one, or ask a captain to invite you.",
              style: TextStyle(color: c.muted),
            )
          else
            for (final team in joined)
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: FsCard(
                  padding: EdgeInsets.zero,
                  child: ListTile(
                    minTileHeight: 64,
                    onTap: () => _open(team.slug),
                    leading: TeamBadge(name: team.name),
                    title: Text(team.name, style: const TextStyle(fontWeight: FontWeight.w500)),
                    subtitle: Text(
                      [count(team.memberCount, 'player'), ?team.myRole?.label].join(' · '),
                      style: TextStyle(fontSize: 14, color: c.muted),
                    ),
                  ),
                ),
              ),
          const SizedBox(height: 32),
          const SectionTitle('Find a team'),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _q,
                  textInputAction: TextInputAction.search,
                  onSubmitted: (_) => _search(),
                  decoration: InputDecoration(
                    hintText: 'Team name',
                    prefixIcon: Icon(LucideIcons.search, size: 18, color: c.subtle),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              OutlinedButton(onPressed: _search, child: const Text('Search')),
            ],
          ),
          const SizedBox(height: 12),
          if (found != null && found.isEmpty)
            Text('No public teams found.', style: TextStyle(color: c.muted))
          else
            for (final team in found ?? const <TeamSummary>[])
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: FsCard(
                  padding: EdgeInsets.zero,
                  child: ListTile(
                    onTap: () => _open(team.slug),
                    title: Text(team.name, style: const TextStyle(fontWeight: FontWeight.w500)),
                    subtitle: Text(
                      [?team.city, ?team.skillLevel?.label, count(team.memberCount, 'player')].join(' · '),
                      style: TextStyle(fontSize: 14, color: c.muted),
                    ),
                  ),
                ),
              ),
        ],
      ),
    );
  }
}

/// A team's first letter on the brand green, until teams have logos. The light theme's darker green in both themes,
/// so the white letter stays readable.
class TeamBadge extends StatelessWidget {
  const TeamBadge({super.key, required this.name, this.size = 40});

  final String name;
  final double size;

  @override
  Widget build(BuildContext context) {
    return ExcludeSemantics(
      child: Container(
        width: size,
        height: size,
        alignment: Alignment.center,
        decoration: BoxDecoration(color: FsColors.light.brand, borderRadius: BorderRadius.circular(size / 4)),
        child: Text(
          name.characters.first.toUpperCase(),
          style: TextStyle(fontSize: size * 0.45, fontWeight: FontWeight.w700, color: Colors.white),
        ),
      ),
    );
  }
}
