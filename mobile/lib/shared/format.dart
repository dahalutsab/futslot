/// Display helpers, in the app's language (see [l]). Times are shown in the venue's time zone, 12-hour, the way
/// people in Nepal say them; in Nepali with Devanagari digits and Bikram Sambat dates. Phone numbers and
/// transaction ids stay in Latin digits, so they can be copied and typed.
library;

import 'package:intl/intl.dart';
import 'package:nepali_utils/nepali_utils.dart';
import 'package:timezone/timezone.dart' as tz;

import '../core/api_types.dart';
import '../l10n/l10n.dart';

final _amountEn = NumberFormat.decimalPattern('en_IN')..maximumFractionDigits = 2;

final _amountNe = NumberFormat.decimalPattern('ne')..maximumFractionDigits = 2;

/// "1,200", "1,20,000" (lakh grouping); "१,२००" in Nepali.
String amount(num value) => (inNepali ? _amountNe : _amountEn).format(value);

/// "Rs 1,200", "Rs 1,20,000" (lakh grouping); "रु १,२००" in Nepali.
String rupees(num? value) => '${inNepali ? 'रु' : 'Rs'} ${amount(value ?? 0)}';

/// "6 PM", "6:30 PM" from minutes past midnight (values past 1440 roll into the next day). In Nepali the part of the
/// day comes first, as it's said: "बिहान ६ बजे", "बेलुका ६:३० बजे".
String clock(int minutes) {
  final inDay = ((minutes % 1440) + 1440) % 1440;
  final hours = inDay ~/ 60;
  final mins = inDay % 60;
  final hour12 = hours % 12 == 0 ? 12 : hours % 12;
  final time = mins == 0 ? '$hour12' : '$hour12:${mins.toString().padLeft(2, '0')}';
  if (inNepali) {
    final part = switch (hours) {
      >= 4 && < 12 => 'बिहान',
      >= 12 && < 16 => 'दिउँसो',
      >= 16 && < 20 => 'बेलुका',
      _ => 'राति',
    };
    return '$part ${digits(time)} बजे';
  }
  return '$time ${hours < 12 ? 'AM' : 'PM'}';
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

/// "Thu 9 Oct" for a YYYY-MM-DD; "बिहि, २३ असोज" (Bikram Sambat) in Nepali.
String dayLabel(String day) =>
    inNepali ? _bs(_utcDay(day), 'EE, d MMMM') : DateFormat('EEE d MMM').format(_utcDay(day));

/// "Thu" for a YYYY-MM-DD; "बिहि" in Nepali.
String weekdayShort(String day) => inNepali ? _bs(_utcDay(day), 'EE') : DateFormat('EEE').format(_utcDay(day));

/// "9 Oct" for a YYYY-MM-DD; "२३ असोज" in Nepali.
String shortDate(String day) => inNepali ? _bs(_utcDay(day), 'd MMMM') : DateFormat('d MMM').format(_utcDay(day));

/// "9 Oct 2026" for a YYYY-MM-DD; "२३ असोज २०८३" in Nepali.
String fullDate(String day) =>
    inNepali ? _bs(_utcDay(day), 'd MMMM y') : DateFormat('d MMM y').format(_utcDay(day));

/// A calendar day (its year, month and day as written) in Bikram Sambat. Noon, so the conversion's Nepal-time
/// shift can't move it to another day.
String _bs(DateTime day, String pattern) => NepaliDateFormat(pattern, Language.nepali)
    .format(DateTime.utc(day.year, day.month, day.day, 6).toNepaliDateTime());

/// The weekday (1 = Monday ... 7 = Sunday) of a YYYY-MM-DD.
int weekdayOf(String day) => _utcDay(day).weekday;

/// How each wallet spells its own name.
String methodLabel(PaymentMethod method) => switch (method) {
      PaymentMethod.cash => l.methodCash,
      PaymentMethod.esewa => 'eSewa',
      PaymentMethod.khalti => 'Khalti',
      PaymentMethod.fonepay => 'Fonepay',
      PaymentMethod.bankTransfer => l.methodBankTransfer,
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
    return l.justNow;
  }
  if (minutes < 60) {
    return l.minutesAgo(digits(minutes));
  }
  if (minutes < 24 * 60) {
    return l.hoursAgo(digits((minutes / 60).round()));
  }
  return shortDate(_ymd(then.toLocal()));
}
