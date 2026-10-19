/// Booking rules in plain words and the cancel-fee preview, mirroring the server (and the player web app's
/// booking-model.ts).
library;

import 'dart:math';

import '../core/api_types.dart';
import '../l10n/l10n.dart';
import 'format.dart';

/// The week as people in Nepal count it, Sunday first.
List<({Weekday day, String label})> get week => [
      (day: Weekday.sunday, label: l.sunday),
      (day: Weekday.monday, label: l.monday),
      (day: Weekday.tuesday, label: l.tuesday),
      (day: Weekday.wednesday, label: l.wednesday),
      (day: Weekday.thursday, label: l.thursday),
      (day: Weekday.friday, label: l.friday),
      (day: Weekday.saturday, label: l.saturday),
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

/// "10" or "12.5", for percentages the venue set ("१०" in Nepali).
String percent(double value) => digits(value == value.roundToDouble() ? value.toInt() : value);

/// The venue's booking rules, said to the player.
List<String> policyLines(VenuePolicy policy) {
  final lines = <String>[
    policy.advancePercent == 0
        ? l.policyPayAtVenue
        : l.policyAdvance(percent(policy.advancePercent), policy.bookingHoldMinutes),
  ];
  final tiers = _byHoursBefore(policy.cancellationTiers);
  if (tiers.isEmpty) {
    lines.add(l.policyFreeCancelAnyTime);
  } else {
    final fees = [for (final tier in tiers) l.feeWithin(percent(tier.feePercent), l.hours(tier.withinHours))];
    lines.add(l.policyFreeBefore(l.hours(tiers.first.withinHours)));
    lines.add(l.policyLateFee(listOf(fees), policy.depositRequired ? l.policyFeeDeposit : l.policyFeeCapped));
  }
  if (policy.depositRequired) {
    final top = tiers.fold(0.0, (most, tier) => max(most, tier.feePercent));
    lines.add([l.policyDeposit(rupees(depositAmount)), if (top > 0) l.policyNoShow(percent(top))].join(' '));
  }
  final strikeLimit = policy.strikeLimit;
  final strikeAdvance = policy.strikeAdvancePercent;
  if (strikeLimit != null && strikeAdvance != null && strikeAdvance > policy.advancePercent) {
    lines.add(l.policyStrikes(strikeLimit, digits(strikeWindowDays), percent(strikeAdvance)));
  }
  lines.add(l.policyVenueCancels);
  return lines;
}

/// "a", "a and b", "a, b and c".
String listOf(List<String> items) =>
    items.length == 1 ? items.first : l.listAnd(items.sublist(0, items.length - 1).join(', '), items.last);

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
    return l.closed;
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
