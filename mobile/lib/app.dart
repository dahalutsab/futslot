import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/badges.dart';
import 'core/theme.dart';
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

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'FutSlot',
      debugShowCheckedModeBanner: false,
      theme: buildTheme(Brightness.light),
      darkTheme: buildTheme(Brightness.dark),
      themeMode: themeModeOf(ref.watch(themeChoiceProvider)),
      routerConfig: ref.watch(routerProvider),
    );
  }
}
