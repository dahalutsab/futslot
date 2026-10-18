import 'package:flutter_test/flutter_test.dart';
import 'package:futslot/shared/format.dart';
import 'package:timezone/data/latest_10y.dart' as tzdata;

void main() {
  setUpAll(tzdata.initializeTimeZones);

  test('rupees use lakh grouping', () {
    expect(rupees(1200), 'Rs 1,200');
    expect(rupees(120000), 'Rs 1,20,000');
    expect(rupees(99.5), 'Rs 99.5');
    expect(rupees(null), 'Rs 0');
  });

  test('times read the way people say them', () {
    expect(clock(18 * 60), '6 PM');
    expect(clock(0), '12 AM');
    expect(clock(12 * 60 + 30), '12:30 PM');
    expect(clock(1440 + 60), '1 AM');
    expect(localClock('2026-10-09T18:30:00+05:45'), '6:30 PM');
  });

  test('days are counted in the venue time zone', () {
    // 20:00 UTC is already the next morning in Kathmandu (+05:45)
    final evening = DateTime.utc(2026, 10, 9, 20);
    expect(today('Asia/Kathmandu', evening), '2026-10-10');
    expect(clockAt('2026-10-09T20:00:00Z', 'Asia/Kathmandu'), '1:45 AM');
    expect(addDays('2026-10-31', 1), '2026-11-01');
    expect(dayLabel('2026-10-09'), 'Fri 9 Oct');
    expect(weekdayOf('2026-10-11'), DateTime.sunday);
  });

  test('phone numbers keep the country code apart', () {
    expect(phone('+9779856789012'), '+977 9856789012');
    expect(phone(null), '');
  });
}
