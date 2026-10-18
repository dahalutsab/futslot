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
import '../../shared/labels.dart';
import '../../shared/widgets.dart';
import 'teams_screen.dart';

/// One team: its squad, and for members their role. Captains and vice-captains invite players by phone and remove
/// them; only the captain changes roles (handing the captaincy over makes them vice-captain).
class TeamScreen extends ConsumerStatefulWidget {
  const TeamScreen({super.key, required this.slug});

  final String slug;

  @override
  ConsumerState<TeamScreen> createState() => _TeamScreenState();
}

class _TeamScreenState extends ConsumerState<TeamScreen> {
  Team? _team;

  final _phone = TextEditingController();

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
    _phone.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final team = await _api.team(widget.slug);
      if (mounted) {
        setState(() {
          _team = team;
          _error = null;
        });
      }
    } catch (error) {
      if (mounted) {
        setState(() => _error = problemOf(error).message);
      }
    }
  }

  Future<void> _act(Future<void> Function(Team team) action) async {
    final team = _team;
    if (team == null || _busy) {
      return;
    }
    FocusScope.of(context).unfocus();
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await action(team);
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

  Future<void> _set(Future<Team> call) async {
    final team = await call;
    if (mounted) {
      setState(() => _team = team);
    }
  }

  Future<void> _invite() {
    final digits = _phone.text.replaceAll(RegExp(r'\D'), '');
    if (digits.length < 10) {
      setState(() => _error = 'Enter their 10-digit mobile number.');
      return Future.value();
    }
    return _act((team) async {
      await _set(_api.invite(team.id, digits.substring(digits.length - 10)));
      _phone.clear();
    });
  }

  Future<void> _answer(bool accept) => _act((team) async {
        await _api.answerInvite(team.id, accept);
        if (accept) {
          await _load();
        } else if (mounted) {
          context.pop();
        }
      });

  Future<void> _leave() => _act((team) async {
        await _api.leaveTeam(team.id);
        if (mounted) {
          context.pop();
        }
      });

  @override
  Widget build(BuildContext context) {
    final team = _team;
    return Scaffold(
      appBar: AppBar(title: Text(team?.name ?? 'Team')),
      body: switch ((team, _error)) {
        (null, final String error) => ScreenBody(children: [ErrorPanel(message: error, onRetry: _load)]),
        (final Team t, _) => ScreenBody(onRefresh: _load, children: _body(context, t)),
        _ => const ScreenBody(children: [Skeleton(height: 192)]),
      },
    );
  }

  List<Widget> _body(BuildContext context, Team t) {
    final c = context.fs;
    final me = ref.watch(sessionProvider)?.user.id;
    final captain = t.joined && t.myRole == TeamRole.captain;
    return [
      Row(
        children: [
          TeamBadge(name: t.name, size: 56),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(t.name, style: Theme.of(context).textTheme.headlineSmall),
                Text(
                  [?t.city, ?t.skillLevel?.label, count(t.memberCount, 'player')].join(' · '),
                  style: TextStyle(color: c.muted),
                ),
              ],
            ),
          ),
        ],
      ),
      if (_error case final error?) ...[const SizedBox(height: 16), Notice(error)],
      if (t.myStatus == MemberStatus.invited) ...[
        const SizedBox(height: 20),
        FsCard(
          borderColor: c.held.line,
          child: Row(
            children: [
              Expanded(child: Text("You're invited to join ${t.name}.")),
              FilledButton(onPressed: _busy ? null : () => _answer(true), child: const Text('Join')),
              TextButton(
                onPressed: _busy ? null : () => _answer(false),
                style: TextButton.styleFrom(foregroundColor: c.muted),
                child: const Text('Decline'),
              ),
            ],
          ),
        ),
      ],
      const SizedBox(height: 24),
      const SectionTitle('Squad'),
      FsCard(
        padding: EdgeInsets.zero,
        child: Column(
          children: [
            for (final (i, m) in t.members.indexed) ...[
              if (i > 0) const Divider(),
              ListTile(
                minTileHeight: 56,
                title: Text.rich(
                  TextSpan(
                    text: m.name,
                    children: [if (m.userId == me) TextSpan(text: ' (you)', style: TextStyle(color: c.muted))],
                  ),
                ),
                subtitle: Text(
                  m.status == MemberStatus.invited ? 'Invited' : m.role.label,
                  style: TextStyle(fontSize: 14, color: c.muted),
                ),
                trailing: _memberMenu(t, m, me: me, captain: captain),
              ),
            ],
          ],
        ),
      ),
      if (t.leads) ...[
        const SizedBox(height: 20),
        FsCard(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('Invite a player', style: Theme.of(context).textTheme.titleMedium),
              Text(
                "By the phone number they use FutSlot with. They're told and can join.",
                style: TextStyle(fontSize: 14, color: c.muted),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _phone,
                      keyboardType: TextInputType.phone,
                      inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9+ ]'))],
                      style: const TextStyle(fontFeatures: tabular),
                      decoration: const InputDecoration(hintText: '98XXXXXXXX'),
                      onSubmitted: (_) => _invite(),
                    ),
                  ),
                  const SizedBox(width: 8),
                  FilledButton(onPressed: _busy ? null : _invite, child: const Text('Invite')),
                ],
              ),
            ],
          ),
        ),
      ],
      if (t.joined && !captain) ...[
        const SizedBox(height: 24),
        Align(
          alignment: Alignment.centerLeft,
          child: TextButton(
            onPressed: _busy ? null : _leave,
            style: TextButton.styleFrom(foregroundColor: c.danger),
            child: Text('Leave ${t.name}'),
          ),
        ),
      ],
      if (captain && t.memberCount > 1) ...[
        const SizedBox(height: 24),
        Text('To leave, make someone else captain first.', style: TextStyle(fontSize: 14, color: c.subtle)),
      ],
    ];
  }

  /// What a leader can do to another member: the captain changes roles, leaders remove players or cancel invites.
  Widget? _memberMenu(Team t, TeamMember m, {required String? me, required bool captain}) {
    final roles = captain && m.userId != me && m.status == MemberStatus.active;
    final remove = t.leads && m.userId != me && m.role != TeamRole.captain;
    if (!roles && !remove) {
      return null;
    }
    return PopupMenuButton<VoidCallback>(
      tooltip: 'Change ${m.name}',
      icon: Icon(LucideIcons.ellipsis, color: context.fs.muted),
      enabled: !_busy,
      onSelected: (action) => action(),
      itemBuilder: (context) => [
        if (roles)
          for (final role in TeamRole.values)
            if (role != m.role)
              PopupMenuItem(
                value: () => _act((team) => _set(_api.changeRole(team.id, m.userId, role))),
                child: Text(role == TeamRole.captain ? 'Make captain' : 'Make ${role.label.toLowerCase()}'),
              ),
        if (remove)
          PopupMenuItem(
            value: () => _act((team) => _set(_api.removeMember(team.id, m.userId))),
            child: Text(m.status == MemberStatus.invited ? 'Cancel invite' : 'Remove'),
          ),
      ],
    );
  }
}
