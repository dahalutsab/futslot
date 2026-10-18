import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:timezone/data/latest_10y.dart' as tzdata;

import 'app.dart';
import 'core/session.dart';
import 'core/theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  tzdata.initializeTimeZones();
  final preferences = await SharedPreferences.getInstance();
  final store = SessionStore();
  final session = await store.read();
  runApp(ProviderScope(
    // Screens show their own "Try again"; failed requests aren't retried behind the player's back
    retry: (_, _) => null,
    overrides: [
      preferencesProvider.overrideWithValue(preferences),
      sessionStoreProvider.overrideWithValue(store),
      initialSessionProvider.overrideWithValue(session),
    ],
    child: const FutSlotApp(),
  ));
}
