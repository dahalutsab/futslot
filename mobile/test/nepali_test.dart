import 'package:flutter_test/flutter_test.dart';
import 'package:futslot/core/api_types.dart';
import 'package:futslot/l10n/l10n.dart';
import 'package:futslot/shared/booking_model.dart';
import 'package:futslot/shared/format.dart';
import 'package:futslot/shared/labels.dart';

void main() {
  setUp(() => useLanguage(AppLanguage.ne));
  tearDown(() => useLanguage(AppLanguage.en));

  test('money and counts use Devanagari digits and lakh grouping', () {
    expect(rupees(1200), 'रु १,२००');
    expect(rupees(120000), 'रु १,२०,०००');
    expect(rupees(1250.5), 'रु १,२५०.५');
    expect(l.players(3), '३ खेलाडी');
    expect(digits('6:30'), '६:३०');
  });

  test('times say the part of the day, the way it is said', () {
    expect(clock(6 * 60), 'बिहान ६ बजे');
    expect(clock(6 * 60 + 30), 'बिहान ६:३० बजे');
    expect(clock(13 * 60), 'दिउँसो १ बजे');
    expect(clock(18 * 60), 'बेलुका ६ बजे');
    expect(clock(21 * 60), 'राति ९ बजे');
    expect(clock(0), 'राति १२ बजे');
    expect(localClock('2026-10-19T19:00:00+05:45'), 'बेलुका ७ बजे');
  });

  test('dates are Bikram Sambat', () {
    // Nepali New Year 2083 fell on 14 April 2026
    expect(dayLabel('2026-04-14'), 'मंगल, १ बैशाख');
    expect(dayLabel('2026-04-13'), 'सोम, ३० चैत्र');
    expect(dayLabel('2026-10-19'), 'सोम, २ कार्तिक');
    expect(shortDate('2026-10-19'), '२ कार्तिक');
    expect(fullDate('2027-01-01'), '१७ पौष २०८३');
    expect(weekdayShort('2026-10-24'), 'शनि');
  });

  test('phone numbers stay in Latin digits, to copy and type', () {
    expect(phone('+9779812345678'), '+977 9812345678');
  });

  test('booking rules read in Nepali', () {
    final policy = VenuePolicy(
      advancePercent: 30,
      bookingHoldMinutes: 15,
      cancellationTiers: const [
        CancellationTier(withinHours: 24, feePercent: 50),
        CancellationTier(withinHours: 2, feePercent: 100),
      ],
      maxAdvanceBookingDays: 30,
      strikeLimit: null,
      strikeAdvancePercent: null,
      depositRequired: false,
    );
    expect(policyLines(policy), [
      'पक्का गर्न ३०% अग्रिम तिर्नुहोस्। तपाईंले तिर्दासम्म स्लट १५ मिनेट होल्ड गरिन्छ।',
      'खेलभन्दा २४ घण्टा वा त्योभन्दा अघि रद्द गरे निःशुल्क।',
      'त्यसपछि रद्द गरे बुकिङ मूल्यको केही हिस्सा लाग्छ: २४ घण्टा भित्र ५०% र २ घण्टा भित्र १००%। '
          'तपाईंले तिरेको भन्दा बढी कहिल्यै लाग्दैन।',
      'भेन्युले रद्द गरेमा सबै पैसा फिर्ता हुन्छ।',
    ]);
    expect(planTerms(MembershipType.gamePack, gamesIncluded: 10, validityDays: 60), '१० खेल, ६० दिनको लागि');
  });
}
