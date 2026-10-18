/// Display helpers. Times are shown in the venue's time zone, 12-hour, the way people in Nepal say them.
library;

import 'package:intl/intl.dart';
import 'package:timezone/timezone.dart' as tz;

import '../core/api_types.dart';

final _rupees = NumberFormat.decimalPattern('en_IN')..maximumFractionDigits = 2;

/// "Rs 1,200", "Rs 1,20,000" (lakh grouping).
String rupees(num? amount) => 'Rs ${_rupees.format(amount ?? 0)}';

/// "6 PM", "6:30 PM" from minutes past midnight (values past 1440 roll into the next day).
String clock(int minutes) {
  final inDay = ((minutes % 1440) + 1440) % 1440;
  final hours = inDay ~/ 60;
  final mins = inDay % 60;
  final suffix = hours < 12 ? 'AM' : 'PM';
  final hour12 = hours % 12 == 0 ? 12 : hours % 12;
  return mins == 0 ? '$hour12 $suffix' : '$hour12:${mins.toString().padLeft(2, '0')} $suffix';
}

/// "6 PM" from a venue-local timestamp such as 2026-10-09T18:00:00+05:45, read as written (no zone maths).
String localClock(String iso) {
  final hours = int.parse(iso.substring(11, 13));
  final minutes = int.parse(iso.substring(14, 16));
  return clock(hours * 60 + minutes);
}

tz.Location _zone(String name) {
  try {
    return tz.getLocation(name);
  } catch (_) {
    return tz.getLocation('Asia/Kathmandu');
  }
}

String _ymd(DateTime date) =>
    '${date.year.toString().padLeft(4, '0')}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';

/// Today's date (YYYY-MM-DD) in [timeZone].
String today(String timeZone, [DateTime? now]) => _ymd(tz.TZDateTime.from(now ?? DateTime.now(), _zone(timeZone)));

/// The date (YYYY-MM-DD) an instant falls on in [timeZone].
String dayOf(String instant, String timeZone) => today(timeZone, DateTime.parse(instant));

/// "6:05 PM" for an instant, as the clock reads in [timeZone].
String clockAt(String instant, String timeZone) {
  final local = tz.TZDateTime.from(DateTime.parse(instant), _zone(timeZone));
  return clock(local.hour * 60 + local.minute);
}

DateTime _utcDay(String day) {
  final [y, m, d] = day.split('-').map(int.parse).toList();
  return DateTime.utc(y, m, d);
}

/// YYYY-MM-DD shifted by whole days.
String addDays(String day, int days) => _ymd(_utcDay(day).add(Duration(days: days)));

/// "Thu 9 Oct" for a YYYY-MM-DD.
String dayLabel(String day) => DateFormat('EEE d MMM').format(_utcDay(day));

/// The weekday (1 = Monday ... 7 = Sunday) of a YYYY-MM-DD.
int weekdayOf(String day) => _utcDay(day).weekday;

/// How each wallet spells its own name.
String methodLabel(PaymentMethod method) => switch (method) {
      PaymentMethod.cash => 'Cash',
      PaymentMethod.esewa => 'eSewa',
      PaymentMethod.khalti => 'Khalti',
      PaymentMethod.fonepay => 'Fonepay',
      PaymentMethod.bankTransfer => 'Bank transfer',
    };

/// "+977 9856789012": the country code apart, so the familiar 10 digits stand out.
String phone(String? value) {
  if (value == null) {
    return '';
  }
  return value.startsWith('+977') && value.length == 14 ? '+977 ${value.substring(4)}' : value;
}

/// "just now", "5 min ago", "3 h ago", then "7 Oct".
String ago(String instant, [DateTime? now]) {
  final then = DateTime.parse(instant);
  final minutes = ((now ?? DateTime.now()).difference(then).inSeconds / 60).round();
  if (minutes < 1) {
    return 'just now';
  }
  if (minutes < 60) {
    return '$minutes min ago';
  }
  if (minutes < 24 * 60) {
    return '${(minutes / 60).round()} h ago';
  }
  return DateFormat('d MMM').format(then.toLocal());
}
