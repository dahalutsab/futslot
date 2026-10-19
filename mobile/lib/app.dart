import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/api.dart';
import 'core/badges.dart';
import 'core/session.dart';
import 'core/theme.dart';
import 'l10n/l10n.dart';
import 'router.dart';

class FutSlotApp extends ConsumerStatefulWidget {
  const FutSlotApp({super.key});

  @override
  ConsumerState<FutSlotApp> createState() => _FutSlotAppState();
}

class _FutSlotAppState extends ConsumerState<FutSlotApp> {
  late final AppLifecycleListener _lifecycle;

  @override
  void initState() {
    super.initState();
    // Unread counts catch up as soon as the player comes back to the app
    _lifecycle = AppLifecycleListener(onResume: () => ref.read(badgesProvider.notifier).refresh());
  }

  @override
  void dispose() {
    _lifecycle.dispose();
    super.dispose();
  }

  /// The account keeps the app's language, so notifications and texts come in it too.
  Future<void> _syncLanguage() async {
    final user = ref.read(sessionProvider)?.user;
    final language = ref.read(languageProvider).name;
    if (user == null || user.preferredLanguage == language) {
      return;
    }
    try {
      final updated = await ref
          .read(apiProvider)
          .updateMe(fullName: user.fullName, email: user.email, preferredLanguage: language);
      ref.read(sessionProvider.notifier).updateUser(updated);
    } catch (_) {
      // Tried again next time; the app's own language doesn't depend on it
    }
  }

  @override
  Widget build(BuildContext context) {
    final language = ref.watch(languageProvider);
    ref.listen(sessionProvider.select((session) => session?.user.id), (_, id) {
      final account = ref.read(sessionProvider)?.user.preferredLanguage;
      final languages = ref.read(languageProvider.notifier);
      // A phone that hasn't picked a language takes the account's; one that has tells the account
      if (id != null && !languages.chosen && account == AppLanguage.ne.name) {
        languages.set(AppLanguage.ne);
      } else {
        _syncLanguage();
      }
    });
    ref.listen(languageProvider, (_, _) => _syncLanguage());
    return MaterialApp.router(
      // Everything is rebuilt in the new language: screens read [l], not an inherited widget
      key: ValueKey(language),
      title: 'FutSlot',
      locale: Locale(language.name),
      supportedLocales: S.supportedLocales,
      localizationsDelegates: S.localizationsDelegates,
      debugShowCheckedModeBanner: false,
      theme: buildTheme(Brightness.light),
      darkTheme: buildTheme(Brightness.dark),
      themeMode: themeModeOf(ref.watch(themeChoiceProvider)),
      routerConfig: ref.watch(routerProvider),
    );
  }
}
