import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { VenuePolicy } from '../core/api.types';
import { policyLines } from '../shared/booking-model';
import { calendarParts, clock, dayLabel, fullDate, localClock, phone, rupees, shortDate, weekdayShort } from '../shared/format';
import { planTerms } from '../shared/labels';
import { PlayNav } from '../shared/play-nav';
import { digits, l, setLanguage } from './i18n';

describe('Nepali', () => {
  beforeEach(() => setLanguage('ne', false));
  afterEach(() => setLanguage('en', false));

  it('writes money and counts in Devanagari digits with lakh grouping', () => {
    expect(rupees(1200)).toBe('रु १,२००');
    expect(rupees(120000)).toBe('रु १,२०,०००');
    expect(rupees(1250.5)).toBe('रु १,२५०.५');
    expect(l().players(3)).toBe('३ खेलाडी');
    expect(digits('6:30')).toBe('६:३०');
  });

  it('says the part of the day, the way times are said', () => {
    expect(clock(6 * 60)).toBe('बिहान ६ बजे');
    expect(clock(6 * 60 + 30)).toBe('बिहान ६:३० बजे');
    expect(clock(13 * 60)).toBe('दिउँसो १ बजे');
    expect(clock(18 * 60)).toBe('बेलुका ६ बजे');
    expect(clock(21 * 60)).toBe('राति ९ बजे');
    expect(clock(0)).toBe('राति १२ बजे');
    expect(localClock('2026-10-19T19:00:00+05:45')).toBe('बेलुका ७ बजे');
  });

  it('dates in Bikram Sambat, the same as the mobile app', () => {
    // Nepali New Year 2083 fell on 14 April 2026
    expect(dayLabel('2026-04-14')).toBe('मंगल, १ बैशाख');
    expect(dayLabel('2026-04-13')).toBe('सोम, ३० चैत्र');
    expect(dayLabel('2026-10-19')).toBe('सोम, २ कार्तिक');
    expect(shortDate('2026-10-19')).toBe('२ कार्तिक');
    expect(fullDate('2027-01-01')).toBe('१७ पौष २०८३');
    expect(weekdayShort('2026-10-24')).toBe('शनि');
    expect(calendarParts('2026-10-19')).toEqual({ weekday: 'सोम', day: '२', month: 'कार्तिक' });
  });

  it('keeps phone numbers in Latin digits, to copy and type', () => {
    expect(phone('+9779812345678')).toBe('+977 9812345678');
  });

  it('reads the booking rules in Nepali', () => {
    const policy: VenuePolicy = {
      advancePercent: 30, bookingHoldMinutes: 15,
      cancellationTiers: [{ withinHours: 24, feePercent: 50 }, { withinHours: 2, feePercent: 100 }],
      maxAdvanceBookingDays: 30, strikeLimit: null, strikeAdvancePercent: null, depositRequired: false,
    };
    expect(policyLines(policy)).toEqual([
      'पक्का गर्न ३०% अग्रिम तिर्नुहोस्। तपाईंले तिर्दासम्म स्लट १५ मिनेट होल्ड गरिन्छ।',
      'खेलभन्दा २४ घण्टा वा त्योभन्दा अघि रद्द गरे निःशुल्क।',
      'त्यसपछि रद्द गरे बुकिङ मूल्यको केही हिस्सा लाग्छ: २४ घण्टा भित्र ५०% र २ घण्टा भित्र १००%। तपाईंले तिरेको भन्दा बढी कहिल्यै लाग्दैन।',
      'भेन्युले रद्द गरेमा सबै पैसा फिर्ता हुन्छ।',
    ]);
    expect(planTerms({ type: 'GAME_PACK', gamesIncluded: 10, discountPercent: null }, 60)).toBe('१० खेल, ६० दिनको लागि');
  });

  it('redraws a screen in the other language straight away', async () => {
    setLanguage('en', false);
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(PlayNav);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Tournaments');
    setLanguage('ne', false);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('प्रतियोगिता');
    expect(fixture.nativeElement.textContent).not.toContain('Tournaments');
  });
});
