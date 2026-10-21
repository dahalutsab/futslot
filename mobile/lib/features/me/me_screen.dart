import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/api.dart';
import '../../core/api_error.dart';
import '../../core/session.dart';
import '../../core/theme.dart';
import '../../l10n/l10n.dart';
import '../../shared/format.dart';
import '../../shared/language_switch.dart';
import '../../shared/notification_bell.dart';
import '../../shared/widgets.dart';

/// Everything else the player keeps with FutSlot.
List<({String path, String label, String detail, IconData icon})> get _links => [
      (path: '/me/deposit', label: l.meDeposit, detail: l.meDepositDetail, icon: LucideIcons.wallet),
      (path: '/me/profile', label: l.meProfile, detail: l.meProfileDetail, icon: LucideIcons.shirt),
      (path: '/me/memberships', label: l.meMemberships, detail: l.meMembershipsDetail, icon: LucideIcons.badgeCheck),
      (path: '/me/splits', label: l.meDues, detail: l.meDuesDetail, icon: LucideIcons.coins),
      (path: '/me/favourites', label: l.meFavourites, detail: l.meFavouritesDetail, icon: LucideIcons.heart),
      (path: '/me/reviews', label: l.meReviews, detail: l.meReviewsDetail, icon: LucideIcons.star),
      (path: '/me/waitlist', label: l.meWaitlist, detail: l.meWaitlistDetail, icon: LucideIcons.hourglass),
      (path: '/me/password', label: l.mePassword, detail: l.mePasswordDetail, icon: LucideIcons.keyRound),
    ];

/// The player's account: their details, the deposit and other things they keep, language, theme and signing out.
class MeScreen extends ConsumerStatefulWidget {
  const MeScreen({super.key});

  @override
  ConsumerState<MeScreen> createState() => _MeScreenState();
}

class _MeScreenState extends ConsumerState<MeScreen> {
  late final _user = ref.read(sessionProvider)?.user;

  late final _fullName = TextEditingController(text: _user?.fullName ?? '');

  late final _email = TextEditingController(text: _user?.email ?? '');

  var _saving = false;

  ({bool ok, String text})? _message;

  @override
  void dispose() {
    _fullName.dispose();
    _email.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    final fullName = _fullName.text.trim();
    if (fullName.length < 2) {
      setState(() => _message = (ok: false, text: l.enterName));
      return;
    }
    FocusScope.of(context).unfocus();
    setState(() {
      _saving = true;
      _message = null;
    });
    try {
      final email = _email.text.trim();
      final user = await ref.read(apiProvider).updateMe(
            fullName: fullName,
            email: email.isEmpty ? null : email,
            preferredLanguage: ref.read(languageProvider).name,
          );
      ref.read(sessionProvider.notifier).updateUser(user);
      if (mounted) {
        setState(() => _message = (ok: true, text: l.saved));
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
    final text = Theme.of(context).textTheme;
    final user = ref.watch(sessionProvider)?.user;
    final theme = ref.watch(themeChoiceProvider);
    return Scaffold(
      body: SafeArea(
        bottom: false,
        child: ScreenBody(
          children: [
            Row(
              children: [
                Expanded(child: Text(user?.fullName ?? '', style: text.headlineSmall)),
                const NotificationBell(),
              ],
            ),
            Text(phone(user?.phone), style: TextStyle(color: c.muted, fontFeatures: tabular)),
            const SizedBox(height: 20),
            FsCard(
              padding: EdgeInsets.zero,
              child: Column(
                children: [
                  for (final (index, link) in _links.indexed) ...[
                    if (index > 0) const Divider(),
                    ListTile(
                      minTileHeight: 56,
                      leading: Icon(link.icon, color: c.muted),
                      title: Text(link.label, style: const TextStyle(fontWeight: FontWeight.w500)),
                      subtitle: Text(link.detail, style: TextStyle(fontSize: 14, color: c.muted)),
                      trailing: Icon(LucideIcons.chevronRight, size: 18, color: c.subtle),
                      onTap: () => context.push(link.path),
                    ),
                  ],
                ],
              ),
            ),
            const SizedBox(height: 24),
            FsCard(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(l.yourDetails, style: text.titleMedium),
                  const SizedBox(height: 12),
                  FieldLabel(l.name),
                  TextField(
                    controller: _fullName,
                    maxLength: 100,
                    textCapitalization: TextCapitalization.words,
                    autofillHints: const [AutofillHints.name],
                    decoration: const InputDecoration(counterText: ''),
                  ),
                  const SizedBox(height: 4),
                  Text(l.venuesSeeThis, style: TextStyle(fontSize: 14, color: c.subtle)),
                  const SizedBox(height: 12),
                  FieldLabel(l.email, optional: true),
                  TextField(
                    controller: _email,
                    maxLength: 254,
                    keyboardType: TextInputType.emailAddress,
                    autocorrect: false,
                    autofillHints: const [AutofillHints.email],
                    decoration: const InputDecoration(counterText: ''),
                  ),
                  if (_message case final message?) ...[
                    const SizedBox(height: 12),
                    Notice(message.text, color: message.ok ? c.confirmed.fg : c.danger),
                  ],
                  const SizedBox(height: 12),
                  Align(
                    alignment: Alignment.centerLeft,
                    child: FilledButton(
                      onPressed: _saving ? null : _save,
                      style: FilledButton.styleFrom(minimumSize: const Size(96, 44)),
                      child: Text(_saving ? l.saving : l.save),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            FsCard(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(l.language, style: text.titleMedium),
                  const SizedBox(height: 12),
                  const LanguageSwitch(),
                  const SizedBox(height: 6),
                  Text(l.languageHelp, style: TextStyle(fontSize: 14, color: c.subtle)),
                  const SizedBox(height: 24),
                  Text(l.appearance, style: text.titleMedium),
                  const SizedBox(height: 12),
                  Segmented(
                    options: [
                      (ThemeChoice.light, l.themeLight),
                      (ThemeChoice.dark, l.themeDark),
                      (ThemeChoice.system, l.themeAuto),
                    ],
                    value: theme,
                    height: 36,
                    onChanged: ref.read(themeChoiceProvider.notifier).set,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            TextButton.icon(
              onPressed: () {
                ref.read(sessionProvider.notifier).signOut();
                context.go('/');
              },
              style: TextButton.styleFrom(foregroundColor: c.danger, minimumSize: const Size.fromHeight(48)),
              icon: const Icon(LucideIcons.logOut, size: 18),
              label: Text(l.signOut),
            ),
          ],
        ),
      ),
    );
  }
}
