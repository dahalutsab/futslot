import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/session.dart';
import '../../core/theme.dart';
import '../../l10n/l10n.dart';
import '../../shared/widgets.dart';

/// Changing the password, or setting one for a player who signs in with Google. Every other device is signed out.
class PasswordScreen extends ConsumerStatefulWidget {
  const PasswordScreen({super.key});

  @override
  ConsumerState<PasswordScreen> createState() => _PasswordScreenState();
}

class _PasswordScreenState extends ConsumerState<PasswordScreen> {
  final _current = TextEditingController();

  final _next = TextEditingController();

  var _show = false;

  var _saving = false;

  ({bool ok, String text})? _message;

  @override
  void dispose() {
    _current.dispose();
    _next.dispose();
    super.dispose();
  }

  Future<void> _save(bool hasPassword) async {
    final problem = _next.text.runes.length < 8
        ? l.useAtLeast8
        : hasPassword && _current.text.isEmpty
            ? l.enterCurrentPassword
            : null;
    if (problem != null) {
      setState(() => _message = (ok: false, text: problem));
      return;
    }
    FocusScope.of(context).unfocus();
    setState(() {
      _saving = true;
      _message = null;
    });
    try {
      final tokens = await ref
          .read(apiProvider)
          .changePassword(currentPassword: hasPassword ? _current.text : null, newPassword: _next.text);
      ref.read(sessionProvider.notifier).replaceTokens(tokens);
      if (mounted) {
        setState(() {
          _current.clear();
          _next.clear();
          _message = (ok: true, text: l.passwordSaved);
        });
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
    final hasPassword = ref.watch(sessionProvider)?.user.passwordSet ?? true;
    final toggle = IconButton(
      tooltip: _show ? l.hidePasswords : l.showPasswords,
      icon: Icon(_show ? LucideIcons.eyeOff : LucideIcons.eye, size: 18),
      onPressed: () => setState(() => _show = !_show),
    );
    return Scaffold(
      appBar: AppBar(title: Text(hasPassword ? l.mePassword : l.setAPassword)),
      body: AutofillGroup(
        child: ScreenBody(
          maxWidth: 480,
          children: [
            if (!hasPassword) ...[
              Text(
                l.googleOnlyNote,
                style: TextStyle(color: c.muted),
              ),
              const SizedBox(height: 16),
            ],
            if (hasPassword) ...[
              FieldLabel(l.currentPassword),
              TextField(
                controller: _current,
                obscureText: !_show,
                autocorrect: false,
                enableSuggestions: false,
                autofillHints: const [AutofillHints.password],
                textInputAction: TextInputAction.next,
                decoration: InputDecoration(suffixIcon: toggle),
              ),
              const SizedBox(height: 16),
            ],
            FieldLabel(l.newPassword),
            TextField(
              controller: _next,
              obscureText: !_show,
              autocorrect: false,
              enableSuggestions: false,
              autofillHints: const [AutofillHints.newPassword],
              textInputAction: TextInputAction.done,
              onSubmitted: (_) => _save(hasPassword),
              decoration: InputDecoration(suffixIcon: hasPassword ? null : toggle),
            ),
            const SizedBox(height: 6),
            Text(
              l.passwordStayHere,
              style: TextStyle(fontSize: 14, color: c.subtle),
            ),
            if (_message case final message?) ...[
              const SizedBox(height: 16),
              Notice(message.text, color: message.ok ? c.confirmed.fg : null),
            ],
            const SizedBox(height: 16),
            Align(
              alignment: Alignment.centerLeft,
              child: FilledButton(
                onPressed: _saving ? null : () => _save(hasPassword),
                child: BusyLabel(
                  busy: _saving,
                  label: hasPassword ? l.changePassword : l.setPassword,
                  busyLabel: l.saving,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
