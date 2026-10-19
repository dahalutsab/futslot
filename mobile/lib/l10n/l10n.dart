/// The app's words, numbers and dates in the player's language: English, or Nepali with Devanagari digits and
/// Bikram Sambat dates.
///
/// Screens and the plain helpers in lib/shared read [l] rather than `S.of(context)`, so a label or a formatted
/// price doesn't need a BuildContext. That's safe because the whole app is rebuilt when the language changes (see
/// FutSlotApp), so nothing keeps showing the old words.
library;

import 'dart:ui';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/theme.dart';
import 'strings.dart';

export 'strings.dart';

enum AppLanguage { en, ne }

S _current = lookupS(const Locale('en'));

/// The words in the language the app is showing.
S get l => _current;

/// Whether the app is in Nepali: Devanagari digits and Bikram Sambat dates.
bool get inNepali => _current.localeName == 'ne';

const _devanagari = ['०', '१', '२', '३', '४', '५', '६', '७', '८', '९'];

/// Writes the digits in [text] in Devanagari when the app is in Nepali: "6:30" → "६:३०".
String digits(Object text) =>
    inNepali ? text.toString().replaceAllMapped(RegExp(r'\d'), (m) => _devanagari[int.parse(m[0]!)]) : text.toString();

/// Switches [l]; [languageProvider] does this, and tests can too.
void useLanguage(AppLanguage language) => _current = lookupS(Locale(language.name));

/// Asks the API to answer in the app's language: its error messages are shown to the player as they come.
final languageInterceptor = InterceptorsWrapper(onRequest: (options, handler) {
  options.headers['Accept-Language'] = _current.localeName;
  handler.next(options);
});

final languageProvider = NotifierProvider<LanguageNotifier, AppLanguage>(LanguageNotifier.new);

/// English or Nepali; remembered on the phone, starting from the phone's own language. The account keeps a copy
/// (`preferredLanguage`) so notifications come in the same language.
class LanguageNotifier extends Notifier<AppLanguage> {
  static const _key = 'futslot.language';

  @override
  AppLanguage build() {
    final saved = ref.read(preferencesProvider).getString(_key);
    final phone = PlatformDispatcher.instance.locale.languageCode == 'ne' ? AppLanguage.ne : AppLanguage.en;
    final language = AppLanguage.values.where((language) => language.name == saved).firstOrNull ?? phone;
    useLanguage(language);
    return language;
  }

  /// Whether the player picked a language on this phone, rather than it following the phone's.
  bool get chosen => ref.read(preferencesProvider).containsKey(_key);

  void set(AppLanguage language) {
    useLanguage(language);
    state = language;
    ref.read(preferencesProvider).setString(_key, language.name);
  }
}
