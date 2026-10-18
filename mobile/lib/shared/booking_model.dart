/// Booking rules in plain words and the cancel-fee preview, mirroring the server (and the player web app's
/// booking-model.ts).
library;

import 'dart:math';

import '../core/api_types.dart';
import 'format.dart';

/// The week as people in Nepal count it, Sunday first.
const week = [
  (day: Weekday.sunday, label: 'Sunday'),
  (day: Weekday.monday, label: 'Monday'),
  (day: Weekday.tuesday, label: 'Tuesday'),
  (day: Weekday.wednesday, label: 'Wednesday'),
  (day: Weekday.thursday, label: 'Thursday'),
  (day: Weekday.friday, label: 'Friday'),
  (day: Weekday.saturday, label: 'Saturday'),
];

/// Mirrors the server: the fee of the closest tier the notice falls inside, 0 at 24 hours or more.
double feePercent(List<CancellationTier> tiers, Duration notice) {
  var fee = 0.0;
  for (final tier in _byHoursBefore(tiers)) {
    if (notice < Duration(hours: tier.withinHours)) {
      fee = tier.feePercent;
    }
  }
  return fee;
}

class CancelOutcome {
  const CancelOutcome({
    required this.feePercent,
    required this.fee,
    required this.kept,
    required this.refund,
    required this.fromDeposit,
  });

  final double feePercent;

  /// The fee in rupees: a share of the booking price.
  final double fee;

  /// What the venue keeps of what was paid.
  final double kept;

  /// What comes back.
  final double refund;

  /// What the deposit pays on top, at most (only for bookings it backs).
  final double fromDeposit;
}

/// What cancelling [booking] at [now] costs, the way the server works it out.
CancelOutcome cancelOutcome(Booking booking, VenuePolicy policy, DateTime now) {
  final percent = feePercent(policy.cancellationTiers, DateTime.parse(booking.startsAt).difference(now));
  final fee = (booking.totalAmount * percent).round() / 100;
  final kept = min(fee, booking.paidAmount);
  return CancelOutcome(
    feePercent: percent,
    fee: fee,
    kept: kept,
    refund: booking.paidAmount - kept,
    fromDeposit: booking.depositBacked ? min(fee - kept, depositAmount) : 0,
  );
}

/// "10" or "12.5", for percentages the venue set.
String percent(double value) => value == value.roundToDouble() ? value.toInt().toString() : value.toString();

/// The venue's booking rules, said to the player.
List<String> policyLines(VenuePolicy policy) {
  final lines = <String>[
    policy.advancePercent == 0
        ? 'Book now and pay at the venue.'
        : 'Pay ${percent(policy.advancePercent)}% up front to confirm. We hold the slot for '
            '${policy.bookingHoldMinutes} minutes while you pay.',
  ];
  final tiers = _byHoursBefore(policy.cancellationTiers);
  if (tiers.isEmpty) {
    lines.add('Cancel any time before the game for free.');
  } else {
    final fees = [for (final tier in tiers) '${percent(tier.feePercent)}% within ${tier.withinHours} ${_hours(tier.withinHours)}'];
    final listed = fees.length == 1 ? fees.first : '${fees.sublist(0, fees.length - 1).join(', ')} and ${fees.last}';
    lines.add('Cancel ${tiers.first.withinHours} ${_hours(tiers.first.withinHours)} or more before the game for free.');
    lines.add('Cancelling later costs a share of the booking price: $listed. ${policy.depositRequired
        ? 'What you paid covers it first; your FutSlot deposit covers the rest.'
        : "It's never more than you've paid."}');
  }
  if (policy.depositRequired) {
    final top = tiers.fold(0.0, (most, tier) => max(most, tier.feePercent));
    lines.add('You need a ${rupees(depositAmount)} FutSlot booking deposit to book here.'
        '${top > 0 ? ' Not showing up costs ${percent(top)}% of the booking price.' : ''}');
  }
  final strikeLimit = policy.strikeLimit;
  final strikeAdvance = policy.strikeAdvancePercent;
  if (strikeLimit != null && strikeAdvance != null && strikeAdvance > policy.advancePercent) {
    final strikes = strikeLimit == 1 ? 'a late cancellation or no-show' : '$strikeLimit late cancellations or no-shows';
    lines.add('After $strikes here in $strikeWindowDays days, you pay ${percent(strikeAdvance)}% up front.');
  }
  lines.add('If the venue cancels, you get everything back.');
  return lines;
}

enum ChipState { confirmed, held, verify, completed, noshow, cancelled, expired }

/// How a booking shows in lists. [checking]: a payment for it is waiting to be checked.
ChipState bookingChip(Booking booking, {bool checking = false}) => switch (booking.status) {
      BookingStatus.pendingPayment => checking ? ChipState.verify : ChipState.held,
      BookingStatus.confirmed => ChipState.confirmed,
      BookingStatus.completed => ChipState.completed,
      BookingStatus.noShow => ChipState.noshow,
      BookingStatus.cancelled => ChipState.cancelled,
      BookingStatus.expired => ChipState.expired,
    };

/// Whether the player can still cancel it themselves: live and not started.
bool cancellable(Booking booking, DateTime now) =>
    (booking.status == BookingStatus.pendingPayment || booking.status == BookingStatus.confirmed) &&
    DateTime.parse(booking.startsAt).isAfter(now);

/// "6 AM – 10 PM", "Closed", for one day's opening hours.
String hoursLabel(DayHours? hours) {
  final open = hours?.openTime;
  final close = hours?.closeTime;
  if (hours == null || hours.closed || open == null || close == null) {
    return 'Closed';
  }
  return '${timeLabel(open)} – ${timeLabel(close)}';
}

/// "6 AM", "6:30 PM" from "HH:mm:ss".
String timeLabel(String time) {
  final [h, m, ...] = time.split(':').map(int.parse).toList();
  return clock(h * 60 + m);
}

List<CancellationTier> _byHoursBefore(List<CancellationTier> tiers) =>
    [...tiers]..sort((a, b) => b.withinHours.compareTo(a.withinHours));

String _hours(int count) => count == 1 ? 'hour' : 'hours';
