import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:futslot/core/api.dart';
import 'package:futslot/core/session.dart';
import 'package:futslot/core/theme.dart';
import 'package:futslot/features/tournaments/tournament_screen.dart';
import 'package:timezone/data/latest_10y.dart' as tzdata;

import 'fakes.dart';

const _venue = {'id': 'v1', 'name': 'Dhuku', 'slug': null, 'city': 'Lalitpur'};

const _tournament = {
  'id': 't1', 'slug': 'dashain-cup', 'name': 'Dashain Cup', 'venue': _venue, 'description': null, 'rules': null,
  'bannerUrl': null, 'format': 'KNOCKOUT', 'status': 'REGISTRATION_OPEN', 'teamSize': 5, 'maxSquadSize': 8,
  'maxTeams': 8, 'entryFee': 1000.0, 'prizeDetails': null, 'registrationClosesAt': null, 'startsOn': '2026-10-25',
  'endsOn': '2026-10-26', 'acceptingTeams': true, 'teamsIn': 1, 'registrations': <Object>[], 'matches': <Object>[],
  'tables': <Object>[], 'champion': null,
};

const _entry = {
  'tournament': {
    'id': 't1', 'slug': 'dashain-cup', 'name': 'Dashain Cup', 'venue': _venue, 'format': 'KNOCKOUT',
    'status': 'REGISTRATION_OPEN', 'startsOn': '2026-10-25', 'endsOn': '2026-10-26', 'entryFee': 1000.0,
    'teamsIn': 1, 'maxTeams': 8, 'bannerUrl': null,
  },
  'entry': {
    'id': 'e1',
    'team': {'registrationId': 'e1', 'teamId': 'k1', 'name': 'Jhamsikhel Kings', 'slug': 'kings'},
    'status': 'PENDING_PAYMENT',
    'groupName': null,
    'squad': [
      {'userId': 'u1', 'name': 'Ram Shrestha', 'jerseyNumber': 9},
    ],
  },
};

Map<String, Object?> _payment(String status, [String? reason]) =>
    {'id': 'p1', 'amount': 1000.0, 'method': 'ESEWA', 'status': status, 'failureReason': reason};

/// The screen with one entry waiting for its fee, and these payments reported for it.
Future<void> _pump(WidgetTester tester, List<Object> payments) async {
  final container = ProviderContainer(overrides: [
    sessionStoreProvider.overrideWithValue(MemoryStore()),
    initialSessionProvider.overrideWithValue(session('t')),
  ]);
  addTearDown(container.dispose);
  container.read(apiHttpProvider).httpClientAdapter = FakeAdapter((request) => switch (request.path) {
        '/api/v1/tournaments/dashain-cup' => (200, _tournament),
        '/api/v1/me/tournament-entries' => (200, [_entry]),
        '/api/v1/tournament-entries/e1/payments' => (200, payments),
        _ => (200, <Object>[]),
      });
  await tester.pumpWidget(UncontrolledProviderScope(
    container: container,
    child: MaterialApp(theme: buildTheme(Brightness.light), home: const TournamentScreen(slug: 'dashain-cup')),
  ));
  for (var i = 0; i < 3; i++) {
    await tester.runAsync(() => Future<void>.delayed(const Duration(milliseconds: 50)));
    await tester.pump(const Duration(milliseconds: 100));
  }
}

void main() {
  setUpAll(tzdata.initializeTimeZones);

  testWidgets('an entry fee being checked shows as sent, not as still to pay', (tester) async {
    await _pump(tester, [_payment('PENDING')]);

    expect(find.text('Payment sent'), findsOneWidget);
    expect(find.textContaining('Payment being checked'), findsOneWidget);
    expect(find.text("I've paid"), findsNothing);

    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('a turned-down entry fee says why and asks again', (tester) async {
    await _pump(tester, [_payment('FAILED', 'No eSewa payment with that id')]);

    expect(find.text("Your last payment wasn't accepted: No eSewa payment with that id"), findsOneWidget);
    expect(find.text('Payment sent'), findsNothing);
    expect(find.textContaining('Pay the entry fee'), findsOneWidget);

    await tester.pumpWidget(const SizedBox());
  });
}
