import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:futslot/core/api.dart';
import 'package:futslot/core/badges.dart';
import 'package:futslot/core/session.dart';
import 'package:futslot/core/theme.dart';
import 'package:futslot/features/messages/thread_screen.dart';
import 'package:timezone/data/latest_10y.dart' as tzdata;

import 'fakes.dart';

/// A venue the player hasn't written to yet; the first message opens conversation c1.
class _Venue {
  final sent = <RequestOptions>[];

  var started = false;

  (int, Object?) answer(RequestOptions request) {
    final now = DateTime.now().toUtc().toIso8601String();
    Map<String, Object?> message(String body) =>
        {'id': 'm${sent.length}', 'senderName': 'Ram', 'fromYou': true, 'bookingId': null, 'body': body, 'readAt': null, 'createdAt': now};
    return switch ((request.method, request.path)) {
      ('GET', '/api/v1/me/conversations') => (200, [
          if (started)
            {'id': 'c1', 'venueId': 'v1', 'venueName': 'Dhuku', 'venueSlug': 'dhuku', 'lastMessage': 'Hi', 'lastMessageAt': now, 'unread': 2},
          {'id': 'c2', 'venueId': 'v2', 'venueName': 'Lakeside', 'venueSlug': 'lakeside', 'lastMessage': 'Ok', 'lastMessageAt': now, 'unread': 1},
        ]),
      ('GET', '/api/v1/notifications/unread-count') => (200, {'unread': 4}),
      ('POST', final path) when path.endsWith('/messages') => () {
          sent.add(request);
          started = true;
          return (200, message((request.data as Map)['body'] as String));
        }(),
      _ => (200, <Object>[]),
    };
  }
}

void main() {
  setUpAll(tzdata.initializeTimeZones);

  testWidgets('the first message from a booking goes to the venue with it, then it is a reply', (tester) async {
    final venue = _Venue();
    final container = ProviderContainer(overrides: [
      sessionStoreProvider.overrideWithValue(MemoryStore()),
      initialSessionProvider.overrideWithValue(session('t')),
    ]);
    container.read(apiHttpProvider).httpClientAdapter = FakeAdapter(venue.answer);
    await tester.pumpWidget(UncontrolledProviderScope(
      container: container,
      child: MaterialApp(
        theme: buildTheme(Brightness.light),
        home: const ThreadScreen(venueId: 'v1', venueName: 'Dhuku', bookingId: 'b1'),
      ),
    ));
    Future<void> settle() async {
      await tester.runAsync(() => Future<void>.delayed(const Duration(milliseconds: 50)));
      await tester.pump(const Duration(milliseconds: 100));
      await tester.pump(const Duration(milliseconds: 100));
    }

    await settle();
    expect(find.textContaining('Ask Dhuku about courts, prices or this booking'), findsOneWidget);

    await tester.enterText(find.byType(TextField), 'Can we bring our own ball?');
    await tester.tap(find.bySemanticsLabel('Send'));
    await settle();
    await settle();
    await tester.enterText(find.byType(TextField), 'Thanks');
    await tester.tap(find.bySemanticsLabel('Send'));
    await settle();

    expect(venue.sent.map((r) => r.path), ['/api/v1/venues/v1/messages', '/api/v1/me/conversations/c1/messages']);
    expect(venue.sent.first.data, {'body': 'Can we bring our own ball?', 'bookingId': 'b1'});
    expect(find.text('Can we bring our own ball?'), findsOneWidget);
    expect(find.text('Thanks'), findsOneWidget);

    await tester.pumpWidget(const SizedBox());
    container.dispose();
  });

  test('unread counts add up messages across conversations', () async {
    final venue = _Venue()..started = true;
    final container = ProviderContainer(overrides: [
      sessionStoreProvider.overrideWithValue(MemoryStore()),
      initialSessionProvider.overrideWithValue(session('t')),
    ]);
    addTearDown(container.dispose);
    container.read(apiHttpProvider).httpClientAdapter = FakeAdapter(venue.answer);

    container.read(badgesProvider);
    await container.read(badgesProvider.notifier).refresh();

    expect(container.read(badgesProvider), (notifications: 4, messages: 3));
  });
}
