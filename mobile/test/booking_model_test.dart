import 'package:flutter_test/flutter_test.dart';
import 'package:futslot/core/api_types.dart';
import 'package:futslot/shared/booking_model.dart';

// The same cases as the player web app's booking-model.spec.ts, so both apps say the same thing.

const _policy = VenuePolicy(
  advancePercent: 30,
  bookingHoldMinutes: 15,
  cancellationTiers: [CancellationTier(withinHours: 6, feePercent: 80), CancellationTier(withinHours: 24, feePercent: 20)],
  maxAdvanceBookingDays: 30,
  strikeLimit: null,
  strikeAdvancePercent: null,
  depositRequired: false,
);

VenuePolicy _with({double? advancePercent, List<CancellationTier>? tiers, bool? depositRequired, int? strikeLimit, double? strikeAdvancePercent}) =>
    VenuePolicy(
      advancePercent: advancePercent ?? _policy.advancePercent,
      bookingHoldMinutes: _policy.bookingHoldMinutes,
      cancellationTiers: tiers ?? _policy.cancellationTiers,
      maxAdvanceBookingDays: _policy.maxAdvanceBookingDays,
      strikeLimit: strikeLimit,
      strikeAdvancePercent: strikeAdvancePercent,
      depositRequired: depositRequired ?? false,
    );

Booking _booking({String status = 'CONFIRMED', bool depositBacked = false}) => Booking.fromJson({
      'id': 'b1',
      'reference': 'FS-ABC123',
      'venue': {'id': 'v', 'name': 'V', 'slug': 'v'},
      'court': {'id': 'c1', 'name': 'Court A'},
      'status': status,
      'startsAt': '2026-10-20T18:00:00+05:45',
      'endsAt': '2026-10-20T19:00:00+05:45',
      'priceAmount': 1200,
      'addOns': <Object>[],
      'discountAmount': 0,
      'totalAmount': 1200,
      'advanceRequired': 360,
      'paidAmount': 360,
      'holdExpiresAt': null,
      'notes': null,
      'cancellationReason': null,
      'cancellationFeePercent': 0,
      'depositBacked': depositBacked,
    });

void main() {
  test('finds the fee the way the server does', () {
    expect(feePercent(_policy.cancellationTiers, const Duration(hours: 30)), 0);
    expect(feePercent(_policy.cancellationTiers, const Duration(hours: 24)), 0);
    expect(feePercent(_policy.cancellationTiers, const Duration(hours: 23)), 20);
    expect(feePercent(_policy.cancellationTiers, const Duration(hours: 2)), 80);
    expect(feePercent(const [], const Duration(milliseconds: 1)), 0);
  });

  test('keeps at most what was paid, and the deposit covers the rest only when it backs the booking', () {
    final b = _booking();
    final start = DateTime.parse(b.startsAt);
    final late = cancelOutcome(b, _policy, start.subtract(const Duration(hours: 2)));
    expect([late.feePercent, late.fee, late.kept, late.refund, late.fromDeposit], [80, 960, 360, 0, 0]);
    final earlier = cancelOutcome(b, _policy, start.subtract(const Duration(hours: 12)));
    expect([earlier.feePercent, earlier.fee, earlier.kept, earlier.refund, earlier.fromDeposit], [20, 240, 240, 120, 0]);
    expect(cancelOutcome(_booking(depositBacked: true), _policy, start.subtract(const Duration(hours: 2))).fromDeposit, 600);
    expect(cancelOutcome(b, _policy, start.subtract(const Duration(hours: 48))).refund, 360);
  });

  test('tells the player the rules in plain words', () {
    expect(policyLines(_policy), [
      'Pay 30% up front to confirm. We hold the slot for 15 minutes while you pay.',
      'Cancel 24 hours or more before the game for free.',
      "Cancelling later costs a share of the booking price: 20% within 24 hours and 80% within 6 hours. It's never more than you've paid.",
      'If the venue cancels, you get everything back.',
    ]);
    expect(policyLines(_with(advancePercent: 0, tiers: const [])).sublist(0, 2),
        ['Book now and pay at the venue.', 'Cancel any time before the game for free.']);
    expect(policyLines(_with(depositRequired: true)),
        contains('You need a Rs 1,000 FutSlot booking deposit to book here. Not showing up costs 80% of the booking price.'));
    expect(policyLines(_with(strikeLimit: 2, strikeAdvancePercent: 50)),
        contains('After 2 late cancellations or no-shows here in 90 days, you pay 50% up front.'));
  });

  test('shows each booking state, and only live future bookings can be cancelled', () {
    expect(bookingChip(_booking(status: 'PENDING_PAYMENT')), ChipState.held);
    expect(bookingChip(_booking(status: 'PENDING_PAYMENT'), checking: true), ChipState.verify);
    expect(bookingChip(_booking(status: 'EXPIRED')), ChipState.expired);
    final b = _booking();
    final start = DateTime.parse(b.startsAt);
    expect(cancellable(b, start.subtract(const Duration(hours: 1))), isTrue);
    expect(cancellable(b, start.add(const Duration(hours: 1))), isFalse);
    expect(cancellable(_booking(status: 'CANCELLED'), DateTime(2000)), isFalse);
  });

  test('labels opening hours', () {
    expect(
      hoursLabel(const DayHours(day: Weekday.sunday, openTime: '06:00:00', closeTime: '22:30:00', closed: false)),
      '6 AM – 10:30 PM',
    );
    expect(hoursLabel(const DayHours(day: Weekday.sunday, openTime: null, closeTime: null, closed: true)), 'Closed');
    expect(hoursLabel(null), 'Closed');
  });
}
