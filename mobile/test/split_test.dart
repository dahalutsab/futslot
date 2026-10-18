import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:futslot/core/api.dart';
import 'package:futslot/core/session.dart';
import 'package:futslot/core/theme.dart';
import 'package:futslot/features/split/split_screen.dart';
import 'package:timezone/data/latest_10y.dart' as tzdata;

import 'fakes.dart';

/// A booking with no split yet; setting one up answers with it, the booker on side A.
class _Booking {
  RequestOptions? setUp;

  (int, Object?) answer(RequestOptions request) => switch ((request.method, request.path)) {
        ('GET', '/api/v1/bookings/b1/split') => (404, {'status': 404, 'code': 'SPLIT_NOT_FOUND', 'detail': 'No split'}),
        ('PUT', '/api/v1/bookings/b1/split') => () {
            setUp = request;
            return (200, {
              'bookingId': 'b1',
              'venueName': 'Dhuku',
              'courtName': 'Court A',
              'startsAt': '2099-10-20T18:00:00+05:45',
              'bookedBy': {'userId': 'u1', 'name': 'Ram S.', 'phone': '+9779800000000'},
              'rule': 'LOSERS_PAY_PERCENT',
              'losersPercent': 80,
              'courtAmount': 1200,
              'extrasAmount': 0,
              'total': 1200,
              'sideAName': 'Reds',
              'sideBName': 'Team B',
              'players': [
                {'id': 'p1', 'name': 'Ram S.', 'side': 'A', 'booker': true, 'you': true, 'settled': true},
              ],
              'outcomes': [
                {'result': 'A_WON', 'sideAEach': 240, 'sideBEach': 960},
              ],
              'collected': 0,
              'outstanding': 0,
              'youAreBooker': true,
            });
          }(),
        _ => (200, <Object>[]),
      };
}

void main() {
  setUpAll(tzdata.initializeTimeZones);

  testWidgets('the booker sets up a baazi split, then sees what each side pays', (tester) async {
    final booking = _Booking();
    final container = ProviderContainer(overrides: [
      sessionStoreProvider.overrideWithValue(MemoryStore()),
      initialSessionProvider.overrideWithValue(session('t')),
    ]);
    container.read(apiHttpProvider).httpClientAdapter = FakeAdapter(booking.answer);
    await tester.pumpWidget(UncontrolledProviderScope(
      container: container,
      child: MaterialApp(theme: buildTheme(Brightness.light), home: const SplitScreen(bookingId: 'b1')),
    ));
    Future<void> settle() async {
      await tester.runAsync(() => Future<void>.delayed(const Duration(milliseconds: 50)));
      await tester.pump(const Duration(milliseconds: 100));
    }

    await settle();
    expect(find.text('Share what this game costs with the players: equally, or baazi-style.'), findsOneWidget);

    await tester.tap(find.text('Losers pay more'));
    await tester.pump();
    await tester.enterText(find.widgetWithText(TextField, '70'), '80');
    await tester.enterText(find.widgetWithText(TextField, 'Team A'), 'Reds');
    await tester.ensureVisible(find.text('Start splitting'));
    await tester.tap(find.text('Start splitting'));
    await settle();

    expect(booking.setUp?.data, {
      'rule': 'LOSERS_PAY_PERCENT',
      'losersPercent': 80,
      'extrasAmount': 0.0,
      'sideAName': 'Reds',
      'bookerSide': 'A',
    });
    expect(find.text('Losers pay more (80%)'), findsOneWidget);
    expect(find.text('Each player pays, roughly'), findsOneWidget);
    expect(find.text('Reds won'), findsOneWidget);

    await tester.pumpWidget(const SizedBox());
    container.dispose();
  });
}
