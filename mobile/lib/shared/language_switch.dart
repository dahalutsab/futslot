import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../l10n/l10n.dart';
import 'widgets.dart';

/// Each language named in itself, so whoever can't read the other one still finds theirs.
const _names = {AppLanguage.en: 'English', AppLanguage.ne: 'नेपाली'};

/// English or नेपाली, switched straight away.
class LanguageSwitch extends ConsumerWidget {
  const LanguageSwitch({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Segmented(
      options: [for (final language in AppLanguage.values) (language, _names[language]!)],
      value: ref.watch(languageProvider),
      height: 36,
      onChanged: ref.read(languageProvider.notifier).set,
    );
  }
}

/// A top-bar button for the other language, for screens before signing in.
class LanguageToggle extends ConsumerWidget {
  const LanguageToggle({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final other = ref.watch(languageProvider) == AppLanguage.en ? AppLanguage.ne : AppLanguage.en;
    return TextButton(
      onPressed: () => ref.read(languageProvider.notifier).set(other),
      child: Text(_names[other]!),
    );
  }
}
