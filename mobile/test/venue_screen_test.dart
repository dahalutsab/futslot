import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/semantics.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:futslot/core/api.dart';
import 'package:futslot/core/session.dart';
import 'package:futslot/core/theme.dart';
import 'package:futslot/features/venue/venue_screen.dart';
import 'package:timezone/data/latest_10y.dart' as tzdata;

import 'fakes.dart';

const _venue = {
  'id': 'v1', 'slug': 'dhuku', 'name': 'Dhuku', 'description': null, 'phone': '+9779812300000', 'email': null,
  'addressLine': 'Jhamsikhel Road', 'area': 'Jhamsikhel', 'city': 'Lalitpur', 'latitude': null, 'longitude': null,
  'timezone': 'Asia/Kathmandu', 'status': 'ACTIVE', 'rejectionReason': null,
  'policy': {
    'advancePercent': 0, 'bookingHoldMinutes': 15, 'cancellationTiers': <Object>[], 'maxAdvanceBookingDays': 30,
    'strikeLimit': null, 'strikeAdvancePercent': null, 'depositRequired': false,
  },
  'paymentInstructions': null, 'averageRating': 0.0, 'reviewCount': 0, 'fromPrice': 1100.0,
  'operatingHours': <Object>[], 'amenities': <Object>[], 'courts': <Object>[], 'photos': <Object>[],
};

const _emptyPage = {'content': <Object>[], 'page': 0, 'size': 10, 'totalElements': 0, 'totalPages': 0};

/// One court with a free slot at 6 and a booked one at 7 on whichever day is asked for.
class _Api {
  final waited = <RequestOptions>[];

  (int, Object?) answer(RequestOptions request) {
    Map<String, Object?> slot(String date, int hour, String status) => {
          'startsAt': '${date}T0$hour:00:00+05:45',
          'endsAt': '${date}T0${hour + 1}:00:00+05:45',
          'price': 1100.0,
          'label': 'STANDARD',
          'status': status,
        };
    return switch ((request.method, request.path)) {
      ('GET', '/api/v1/venues/dhuku') => (200, _venue),
      ('GET', '/api/v1/venues/dhuku/availability') => () {
          final date = request.queryParameters['date'] as String;
          return (200, {
            'venueId': 'v1',
            'date': date,
            'timezone': 'Asia/Kathmandu',
            'courts': [
              {
                'courtId': 'c1',
                'name': 'Court 1',
                'format': 'FIVE_A_SIDE',
                'slotDurationMinutes': 60,
                'slots': [slot(date, 6, 'AVAILABLE'), slot(date, 7, 'BOOKED')],
              },
            ],
          });
        }(),
      ('GET', '/api/v1/venues/dhuku/reviews') => (200, _emptyPage),
      ('POST', '/api/v1/waitlist') => () {
          waited.add(request);
          return (201, null);
        }(),
      _ => (200, <Object>[]),
    };
  }
}

void main() {
  setUpAll(tzdata.initializeTimeZones);

  testWidgets('a screen reader can book a free slot and wait for a booked one', (tester) async {
    final handle = tester.ensureSemantics();
    final api = _Api();
    final container = ProviderContainer(overrides: [
      sessionStoreProvider.overrideWithValue(MemoryStore()),
      initialSessionProvider.overrideWithValue(session('t')),
    ]);
    container.read(apiHttpProvider).httpClientAdapter = FakeAdapter(api.answer);
    await tester.pumpWidget(UncontrolledProviderScope(
      container: container,
      child: MaterialApp(theme: buildTheme(Brightness.light), home: const VenueScreen(slug: 'dhuku')),
    ));
    Future<void> settle() async {
      await tester.runAsync(() => Future<void>.delayed(const Duration(milliseconds: 50)));
      await tester.pump(const Duration(milliseconds: 100));
      await tester.pump(const Duration(milliseconds: 400));
    }

    await settle();
    await settle();

    final free = find.semantics.byLabel(RegExp(r'^6 AM, .*\. Book$')).evaluate().single;
    expect(free.getSemanticsData().hasAction(SemanticsAction.tap), isTrue);

    // Fails unless the slot offers screen readers a tap
    tester.semantics.tap(find.semantics.byLabel('7 AM is booked. Wait for it?'));
    await settle();
    await tester.tap(find.text('Wait for it'));
    await settle();

    expect(api.waited.single.data, containsPair('courtId', 'c1'));
    expect(find.bySemanticsLabel("7 AM, booked. You're waiting for it"), findsOneWidget);

    await tester.pumpWidget(const SizedBox());
    container.dispose();
    handle.dispose();
  });
}
