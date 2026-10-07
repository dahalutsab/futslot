import { Court, DayHours, PricingRule } from '../../core/api.types';
import { gaps, priceWeek, ruleFor, ruleWindow, span, windows } from './pricing-model';

function rule(id: string, change: Partial<PricingRule>): PricingRule {
  return { id, label: 'STANDARD', dayOfWeek: null, startTime: '06:00:00', endTime: '22:00:00', price: 1200, priority: 0,
    validFrom: null, validTo: null, active: true, ...change };
}

const EVERY_DAY = rule('01a', {});
const PEAK = rule('01b', { label: 'PEAK', startTime: '17:00:00', price: 1800 });
const FRIDAY = rule('01c', { dayOfWeek: 'FRIDAY', startTime: '17:00:00', price: 2000 });

describe('pricing model', () => {
  it('picks the rule the API would', () => {
    // 2026-10-09 is a Friday, 2026-10-08 a Thursday
    expect(ruleFor([EVERY_DAY, PEAK], '2026-10-08', 18 * 60)?.price).toBe(1800);
    expect(ruleFor([EVERY_DAY, PEAK, FRIDAY], '2026-10-09', 18 * 60)?.price).toBe(2000);
    expect(ruleFor([EVERY_DAY, PEAK, FRIDAY], '2026-10-08', 10 * 60)?.price).toBe(1200);
    expect(ruleFor([{ ...EVERY_DAY, priority: 5 }, PEAK], '2026-10-08', 18 * 60)?.price).toBe(1200);
    expect(ruleFor([{ ...PEAK, active: false }], '2026-10-08', 18 * 60)).toBeNull();
    expect(ruleFor([{ ...PEAK, validTo: '2026-10-07' }], '2026-10-08', 18 * 60)).toBeNull();
  });

  it('prices late slots as the day they started from', () => {
    const late = rule('01d', { dayOfWeek: 'FRIDAY', startTime: '00:00:00', endTime: '02:00:00', price: 900 });
    expect(ruleFor([late], '2026-10-09', 1440 + 30)?.price).toBe(900);
  });

  it('lays out a week of slots and finds the hours without a price', () => {
    const court: Court = { id: 'c', name: 'Court A', format: 'FIVE_A_SIDE', surface: 'ARTIFICIAL_TURF', indoor: false,
      slotDurationMinutes: 60, status: 'ACTIVE', sortOrder: 0, pricingRules: [EVERY_DAY] };
    const hours: DayHours[] = [
      { day: 'THURSDAY', openTime: '06:00:00', closeTime: '22:00:00', closed: false },
      { day: 'FRIDAY', openTime: '06:00:00', closeTime: '01:00:00', closed: false },
      { day: 'SATURDAY', openTime: '00:00:00', closeTime: '00:00:00', closed: true },
    ];
    const week = priceWeek(court, hours, '2026-10-08', 3);
    expect(week.map((d) => [d.label, d.closed, d.cells.length])).toEqual([['Thu 8 Oct', false, 16], ['Fri 9 Oct', false, 19], ['Sat 10 Oct', true, 0]]);
    const missing = gaps(week, 60);
    expect(missing).toEqual([{ date: '2026-10-09', label: 'Fri 9 Oct', weekday: 'FRIDAY', from: 22 * 60, to: 25 * 60 }]);
    expect(span(missing[0].from, missing[0].to)).toBe('10 PM – 1 AM');
  });

  it('splits late-night windows at midnight', () => {
    expect(windows('17:00', '22:00')).toEqual([{ startTime: '17:00:00', endTime: '22:00:00' }]);
    expect(windows('22:00', '00:00')).toEqual([{ startTime: '22:00:00', endTime: '23:59:59' }]);
    expect(windows('22:00', '01:00')).toEqual([{ startTime: '22:00:00', endTime: '23:59:59' }, { startTime: '00:00:00', endTime: '01:00:00' }]);
    expect(windows('00:00', '00:00')).toEqual([{ startTime: '00:00:00', endTime: '23:59:59' }]);
    expect(ruleWindow({ startTime: '22:00:00', endTime: '23:59:59' })).toBe('10 PM – 12 AM');
  });
});
