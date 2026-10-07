import { discountLabel } from './discount-codes';
import { planSummary } from './membership-plans';

describe('grow labels', () => {
  it('describes plans', () => {
    expect(planSummary({ type: 'GAME_PACK', gamesIncluded: 10, discountPercent: null, validityDays: 90 })).toBe('10 games, valid 90 days');
    expect(planSummary({ type: 'MONTHLY_PASS', gamesIncluded: null, discountPercent: 15, validityDays: 30 }))
      .toBe('15% off on every booking, valid 30 days');
    expect(planSummary({ type: 'MONTHLY_PASS', gamesIncluded: 8, discountPercent: 20, validityDays: 30 }))
      .toBe('20% off for up to 8 games, valid 30 days');
  });

  it('describes discounts', () => {
    expect(discountLabel({ type: 'PERCENT', value: 20, maxDiscountAmount: 300, minBookingAmount: null })).toBe('20% off, up to Rs 300');
    expect(discountLabel({ type: 'FIXED', value: 200, maxDiscountAmount: null, minBookingAmount: 1500 }))
      .toBe('Rs 200 off on bookings of Rs 1,500 or more');
  });
});
