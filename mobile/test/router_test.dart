import 'package:dio/dio.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:futslot/app.dart';
import 'package:futslot/core/api.dart';
import 'package:futslot/core/session.dart';
import 'package:futslot/core/theme.dart';
import 'package:futslot/l10n/l10n.dart';
import 'package:futslot/router.dart';
import 'package:go_router/go_router.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:timezone/data/latest_10y.dart' as tzdata;

import 'fakes.dart';

const _emptyPage = {'content': <Object>[], 'page': 0, 'size': 20, 'totalElements': 0, 'totalPages': 0};

(int, Object?) _api(RequestOptions request) => switch (request.path) {
      '/api/v1/venues' => (200, _emptyPage),
      '/api/v1/auth/options' => (200, {'googleWebClientId': null}),
      '/api/v1/auth/sign-in' => (200, tokens('fresh')),
      '/api/v1/auth/refresh' => (401, {'status': 401, 'detail': 'Refresh token expired'}),
      final path when path.startsWith('/api/v1/bookings/') => (401, {'status': 401, 'detail': 'Expired'}),
      _ => (200, <Object>[]),
    };

Future<ProviderContainer> _app(WidgetTester tester, {StoredSession? signedIn, String? language}) async {
  SharedPreferences.setMockInitialValues({'futslot.language': ?language});
  final preferences = await SharedPreferences.getInstance();
  final container = ProviderContainer(overrides: [
    preferencesProvider.overrideWithValue(preferences),
    sessionStoreProvider.overrideWithValue(MemoryStore()),
    initialSessionProvider.overrideWithValue(signedIn),
    authHttpProvider.overrideWithValue(fakeDio(_api)),
  ]);
  // Only the transport: the real client keeps its token handling
  container.read(apiHttpProvider).httpClientAdapter = FakeAdapter(_api);
  await tester.pumpWidget(UncontrolledProviderScope(container: container, child: const FutSlotApp()));
  await tester.pump();
  return container;
}

GoRouter _router(ProviderContainer container) => container.read(routerProvider);

/// Where the player is, pushed screens included.
String _location(ProviderContainer container) => _router(container).state.uri.toString();

/// Takes the app down before the test ends, so its timers (unread counts, polling) stop with it.
Future<void> _close(WidgetTester tester, ProviderContainer container) async {
  await tester.pumpWidget(const SizedBox());
  container.dispose();
}

/// Lets page transitions finish. Requests go through Dio, whose timeouts are real timers the test clock doesn't
/// run on its own, so they get a moment of real time too.
Future<void> _settle(WidgetTester tester) async {
  await tester.runAsync(() => Future<void>.delayed(const Duration(milliseconds: 50)));
  await tester.pump();
  await tester.pump(const Duration(seconds: 1));
}

void main() {
  setUpAll(tzdata.initializeTimeZones);

  testWidgets('browsing works signed out; bookings ask to sign in, then open', (tester) async {
    final container = await _app(tester);
    expect(find.text('Book a futsal court'), findsOneWidget);

    await tester.tap(find.text('Bookings'));
    await _settle(tester);
    expect(_location(container), '/login?next=%2Fbookings');
    expect(find.text('Sign in to FutSlot'), findsOneWidget);

    await tester.runAsync(() => container.read(sessionProvider.notifier).signIn('9800000000', 'kick-off-at-seven'));
    await _settle(tester);
    await _settle(tester);
    expect(_location(container), '/bookings');
    expect(find.text('No games coming up'), findsOneWidget);
    await _close(tester, container);
  });

  testWidgets('signing in from a screen pushed over another goes on to it', (tester) async {
    final container = await _app(tester);
    _router(container).push('/me/deposit');
    await _settle(tester);
    expect(find.text('Sign in to FutSlot'), findsOneWidget);

    await tester.enterText(find.byType(EditableText).first, '9800000000');
    await tester.enterText(find.byType(EditableText).last, 'kick-off-at-seven');
    await tester.tap(find.text('Sign in'));
    await _settle(tester);
    await _settle(tester);

    expect(_location(container), '/me/deposit');
    expect(find.text('Booking deposit'), findsOneWidget);
    await _close(tester, container);
  });

  testWidgets('a session that runs out on a pushed screen goes back to sign in', (tester) async {
    final container = await _app(tester, signedIn: session('old'));
    _router(container).push('/bookings/b1');
    await _settle(tester);
    await _settle(tester);

    expect(_location(container), '/login?next=%2Fbookings%2Fb1');
    expect(find.text('You were signed out. Please sign in again.'), findsOneWidget);
    await _close(tester, container);
  });

  testWidgets('Play asks to sign in for games, but tournaments are open to everyone', (tester) async {
    final container = await _app(tester);
    await tester.tap(find.text('Play'));
    await _settle(tester);
    expect(_location(container), '/login?next=%2Fmatches');

    _router(container).go('/tournaments');
    await _settle(tester);
    expect(_location(container), '/tournaments');
    expect(find.text('No tournaments coming up'), findsOneWidget);
    await _close(tester, container);
  });

  test('only paths inside the app are followed after signing in', () {
    expect(safeNext('/bookings/1'), '/bookings/1');
    expect(safeNext('//evil.example'), isNull);
    expect(safeNext('https://evil.example'), isNull);
    expect(safeNext(null), isNull);
  });

  testWidgets('the whole app switches language at once', (tester) async {
    final container = await _app(tester, language: 'ne');
    expect(find.text('फुटसल कोर्ट बुक गर्नुहोस्'), findsOneWidget);
    expect(find.text('बुकिङ'), findsOneWidget);

    await tester.tap(find.text('English'));
    await _settle(tester);
    expect(find.text('Book a futsal court'), findsOneWidget);
    expect(find.text('Bookings'), findsOneWidget);
    expect(container.read(preferencesProvider).getString('futslot.language'), 'en');
    useLanguage(AppLanguage.en);
    await _close(tester, container);
  });
}
