import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:futslot/core/google.dart';
import 'package:futslot/core/session.dart';
import 'package:futslot/core/theme.dart';
import 'package:futslot/features/login/login_screen.dart';
import 'package:go_router/go_router.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'fakes.dart';

const _sent = {'expiresInSeconds': 300, 'resendAfterSeconds': 60};

Map<String, Object?> _problem(int status, String code) => {'status': status, 'detail': code, 'code': code};

/// The sign-in screen over a home page, against [answer]. Returns what was sent to the API.
Future<List<RequestOptions>> _pump(WidgetTester tester, (int, Object?) Function(RequestOptions request) answer,
    {String? googleToken}) async {
  final adapter = FakeAdapter(answer);
  SharedPreferences.setMockInitialValues({});
  final preferences = await SharedPreferences.getInstance();
  final container = ProviderContainer(overrides: [
    preferencesProvider.overrideWithValue(preferences),
    sessionStoreProvider.overrideWithValue(MemoryStore()),
    authHttpProvider.overrideWithValue(Dio(BaseOptions(baseUrl: 'http://api.test'))..httpClientAdapter = adapter),
    googleIdTokenProvider.overrideWithValue((clientId) async => googleToken),
  ]);
  addTearDown(container.dispose);
  final router = GoRouter(initialLocation: '/login', routes: [
    GoRoute(path: '/', builder: (context, state) => const Scaffold(body: Text('Home'))),
    GoRoute(path: '/login', builder: (context, state) => const LoginScreen()),
  ]);
  addTearDown(router.dispose);
  await tester.pumpWidget(UncontrolledProviderScope(
    container: container,
    child: MaterialApp.router(theme: buildTheme(Brightness.light), routerConfig: router),
  ));
  await _settle(tester);
  return adapter.requests;
}

Future<void> _settle(WidgetTester tester) async {
  await tester.runAsync(() => Future<void>.delayed(const Duration(milliseconds: 50)));
  await tester.pump();
  await tester.pump(const Duration(milliseconds: 400));
}

Future<void> _tap(WidgetTester tester, String text) async {
  await tester.ensureVisible(find.text(text));
  await tester.tap(find.text(text));
  await _settle(tester);
}

void main() {
  testWidgets('a new player creates an account with a texted code and a password', (tester) async {
    final requests = await _pump(tester, (request) => switch (request.path) {
          '/api/v1/auth/options' => (200, {'googleWebClientId': null}),
          '/api/v1/auth/codes' => (202, _sent),
          '/api/v1/auth/sign-up' => (200, tokens('new')),
          _ => (404, null),
        });
    expect(find.text('Continue with Google'), findsNothing);

    await _tap(tester, 'Create an account');
    await tester.enterText(find.byType(TextField), '98-1234 5678');
    await _tap(tester, 'Send code');
    final fields = find.byType(TextField);
    await tester.enterText(fields.at(0), '123456');
    await tester.enterText(fields.at(1), 'Sita Sharma');
    await tester.enterText(fields.at(2), 'short');
    await _tap(tester, 'Create account');
    expect(find.text('Use at least 8 characters for your password.'), findsOneWidget);

    await tester.enterText(fields.at(2), 'kick-off-at-seven');
    await _tap(tester, 'Create account');

    expect(requests.firstWhere((r) => r.path == '/api/v1/auth/codes').data, {'phone': '9812345678', 'purpose': 'SIGN_UP'});
    expect(requests.last.data, {
      'phone': '9812345678',
      'code': '123456',
      'fullName': 'Sita Sharma',
      'password': 'kick-off-at-seven',
    });
    await _settle(tester);
    expect(find.text('Home'), findsOneWidget);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('an account without a password is sent to set one with a code', (tester) async {
    final requests = await _pump(tester, (request) => switch (request.path) {
          '/api/v1/auth/options' => (200, {'googleWebClientId': null}),
          '/api/v1/auth/sign-in' => (409, _problem(409, 'PASSWORD_NOT_SET')),
          '/api/v1/auth/codes' => (202, _sent),
          _ => (404, null),
        });

    final fields = find.byType(TextField);
    await tester.enterText(fields.at(0), '9800000101');
    await tester.enterText(fields.at(1), 'anything');
    await _tap(tester, 'Sign in');

    expect(find.text('Reset your password'), findsOneWidget);
    expect(find.textContaining("doesn't have a password yet"), findsOneWidget);
    // The number carries over
    await _tap(tester, 'Send code');
    expect(requests.last.data, {'phone': '9800000101', 'purpose': 'RESET_PASSWORD'});
    expect(find.text('New password'), findsOneWidget);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('a first Google sign-in asks for the number, then links it', (tester) async {
    final requests = await _pump(tester, (request) => switch (request.path) {
          '/api/v1/auth/options' => (200, {'googleWebClientId': 'web-client'}),
          '/api/v1/auth/google' => (409, _problem(409, 'GOOGLE_PHONE_NEEDED')),
          '/api/v1/auth/codes' => (202, _sent),
          '/api/v1/auth/google/link' => (200, tokens('google')),
          _ => (404, null),
        }, googleToken: 'id-token');

    await _tap(tester, 'Continue with Google');
    expect(find.text('One more step'), findsOneWidget);
    await tester.enterText(find.byType(TextField), '9800000102');
    await _tap(tester, 'Send code');
    expect(requests.last.data, {'phone': '9800000102', 'purpose': 'LINK_GOOGLE'});
    expect(find.text('Password'), findsNothing);
    await tester.enterText(find.byType(TextField), '654321');
    await _tap(tester, 'Continue');

    expect(requests.last.data, {'idToken': 'id-token', 'phone': '9800000102', 'code': '654321'});
    await _settle(tester);
    expect(find.text('Home'), findsOneWidget);
    await tester.pumpWidget(const SizedBox());
  });
}
