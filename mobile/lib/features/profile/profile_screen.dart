import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/api_types.dart';
import '../../core/theme.dart';
import '../../shared/labels.dart';
import '../../shared/widgets.dart';

/// What other players see when the player asks to join a game or a team: position, level, foot, city.
class ProfileScreen extends ConsumerStatefulWidget {
  const ProfileScreen({super.key});

  @override
  ConsumerState<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends ConsumerState<ProfileScreen> {
  PlayerCard? _card;

  Position? _position;
  SkillLevel? _skill;
  PreferredFoot? _foot;
  final _city = TextEditingController();
  final _bio = TextEditingController();
  var _isPublic = true;

  var _saving = false;

  ({bool ok, String text})? _message;

  Api get _api => ref.read(apiProvider);

  @override
  void initState() {
    super.initState();
    _api.myProfile().then(_show, onError: (Object error) {
      if (mounted) {
        setState(() => _message = (ok: false, text: problemOf(error).message));
      }
    });
  }

  @override
  void dispose() {
    _city.dispose();
    _bio.dispose();
    super.dispose();
  }

  void _show(PlayerCard card) {
    if (!mounted) {
      return;
    }
    setState(() {
      _card = card;
      _position = card.position;
      _skill = card.skillLevel;
      _foot = card.preferredFoot;
      _city.text = card.city ?? '';
      _bio.text = card.bio ?? '';
      _isPublic = card.isPublic ?? true;
    });
  }

  Future<void> _save() async {
    FocusScope.of(context).unfocus();
    setState(() {
      _saving = true;
      _message = null;
    });
    try {
      final card = await _api.saveProfile(
        position: _position,
        skillLevel: _skill,
        preferredFoot: _foot,
        city: _city.text.trim().isEmpty ? null : _city.text.trim(),
        bio: _bio.text.trim().isEmpty ? null : _bio.text.trim(),
        isPublic: _isPublic,
      );
      _show(card);
      if (mounted) {
        setState(() => _message = (ok: true, text: 'Saved.'));
      }
    } catch (error) {
      if (mounted) {
        setState(() => _message = (ok: false, text: problemOf(error).message));
      }
    } finally {
      if (mounted) {
        setState(() => _saving = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = context.fs;
    final card = _card;
    return Scaffold(
      appBar: AppBar(title: const Text('Player profile')),
      body: ScreenBody(
        children: [
          Text(
            'Shown to captains and players when you ask to join a game or a team.',
            style: TextStyle(color: c.muted),
          ),
          const SizedBox(height: 12),
          if (card == null)
            if (_message case final message?) Notice(message.text) else const Skeleton(height: 256)
          else ...[
            Text(
              '${count(card.gamesPlayed, 'game')} played on FutSlot',
              style: TextStyle(fontSize: 14, color: c.muted, fontFeatures: tabular),
            ),
            const SizedBox(height: 16),
            FsCard(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  PillChoices(
                    label: 'Position',
                    options: [for (final p in Position.values) (p, p.label)],
                    value: _position,
                    onChanged: (p) => setState(() => _position = p),
                  ),
                  PillChoices(
                    label: 'Level',
                    options: [for (final s in SkillLevel.values) (s, s.label)],
                    value: _skill,
                    onChanged: (s) => setState(() => _skill = s),
                  ),
                  PillChoices(
                    label: 'Stronger foot',
                    options: [for (final f in PreferredFoot.values) (f, f.label)],
                    value: _foot,
                    onChanged: (f) => setState(() => _foot = f),
                  ),
                  const FieldLabel('City'),
                  TextField(controller: _city, maxLength: 60, decoration: const InputDecoration(counterText: '')),
                  const SizedBox(height: 16),
                  const FieldLabel('About you', optional: true),
                  TextField(
                    controller: _bio,
                    maxLength: 500,
                    minLines: 3,
                    maxLines: 5,
                    textCapitalization: TextCapitalization.sentences,
                    decoration: const InputDecoration(
                      hintText: 'e.g. Quick on the wing, play most Friday evenings',
                      counterText: '',
                    ),
                  ),
                  const SizedBox(height: 8),
                  CheckboxListTile(
                    value: _isPublic,
                    onChanged: (on) => setState(() => _isPublic = on ?? true),
                    controlAffinity: ListTileControlAffinity.leading,
                    contentPadding: EdgeInsets.zero,
                    title: const Text('Let other players see my profile'),
                  ),
                  if (_message case final message?) ...[
                    const SizedBox(height: 8),
                    Notice(message.text, color: message.ok ? c.confirmed.fg : null),
                  ],
                  const SizedBox(height: 12),
                  Align(
                    alignment: Alignment.centerLeft,
                    child: FilledButton(
                      onPressed: _saving ? null : _save,
                      child: BusyLabel(busy: _saving, label: 'Save', busyLabel: 'Saving…'),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }
}
