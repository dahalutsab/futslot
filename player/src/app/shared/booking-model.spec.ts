import { Booking, VenuePolicy } from '../core/api.types';
import { bookingChip, cancellable, cancelOutcome, feePercent, hoursLabel, policyLines } from './booking-model';

const HOUR = 3_600_000;

const POLICY: VenuePolicy = {
  advancePercent: 30, bookingHoldMinutes: 15,
  cancellationTiers: [{ withinHours: 6, feePercent: 80 }, { withinHours: 24, feePercent: 20 }],
  maxAdvanceBookingDays: 30, strikeLimit: null, strikeAdvancePercent: null, depositRequired: false,
};

function booking(overrides: Partial<Booking>): Booking {
  return {
    id: 'b1', reference: 'FS-ABC123', venue: { id: 'v', name: 'V', slug: 'v' }, court: { id: 'c1', name: 'Court A' },
    customer: { id: 'cu', name: 'Ram', phone: '+9779800000000' }, source: 'APP', status: 'CONFIRMED',
    startsAt: '2026-10-20T18:00:00+05:45', endsAt: '2026-10-20T19:00:00+05:45', priceAmount: 1200, addOnsAmount: 0,
    addOns: [], discountAmount: 0, membershipId: null, discountCodeId: null, totalAmount: 1200,
    advanceRequired: 360, paidAmount: 360, paymentStatus: 'PARTIALLY_PAID', holdExpiresAt: null, notes: null,
    cancelledAt: null, cancellationReason: null, cancellationFeePercent: 0, depositBacked: false,
    createdAt: '2026-10-01T00:00:00Z', ...overrides,
  };
}

describe('booking model', () => {
  it('finds the fee the way the server does', () => {
    expect(feePercent(POLICY.cancellationTiers, 30 * HOUR)).toBe(0);
    expect(feePercent(POLICY.cancellationTiers, 24 * HOUR)).toBe(0);
    expect(feePercent(POLICY.cancellationTiers, 23 * HOUR)).toBe(20);
    expect(feePercent(POLICY.cancellationTiers, 2 * HOUR)).toBe(80);
    expect(feePercent([], 1)).toBe(0);
  });

  it('keeps at most what was paid, and the deposit covers the rest only when it backs the booking', () => {
    const b = booking({});
    const start = new Date(b.startsAt).getTime();
    expect(cancelOutcome(b, POLICY, new Date(start - 2 * HOUR)))
      .toEqual({ feePercent: 80, fee: 960, kept: 360, refund: 0, fromDeposit: 0 });
    expect(cancelOutcome(b, POLICY, new Date(start - 12 * HOUR)))
      .toEqual({ feePercent: 20, fee: 240, kept: 240, refund: 120, fromDeposit: 0 });
    expect(cancelOutcome(booking({ depositBacked: true }), POLICY, new Date(start - 2 * HOUR)).fromDeposit).toBe(600);
    expect(cancelOutcome(b, POLICY, new Date(start - 48 * HOUR)).refund).toBe(360);
  });

  it('tells the player the rules in plain words', () => {
    expect(policyLines(POLICY)).toEqual([
      'Pay 30% up front to confirm. We hold the slot for 15 minutes while you pay.',
      'Cancel 24 hours or more before the game for free.',
      "Cancelling later costs a share of the booking price: 20% within 24 hours and 80% within 6 hours. It's never more than you've paid.",
      'If the venue cancels, you get everything back.',
    ]);
    expect(policyLines({ ...POLICY, advancePercent: 0, cancellationTiers: [] }).slice(0, 2))
      .toEqual(['Book now and pay at the venue.', 'Cancel any time before the game for free.']);
    expect(policyLines({ ...POLICY, depositRequired: true })).toContain(
      'You need a Rs 1,000 FutSlot booking deposit to book here. Not showing up costs 80% of the booking price.');
    expect(policyLines({ ...POLICY, strikeLimit: 2, strikeAdvancePercent: 50 })).toContain(
      'After 2 late cancellations or no-shows here in 90 days, you pay 50% up front.');
  });

  it('shows each booking state, and only live future bookings can be cancelled', () => {
    expect(bookingChip(booking({ status: 'PENDING_PAYMENT' }))).toBe('held');
    expect(bookingChip(booking({ status: 'PENDING_PAYMENT' }), true)).toBe('verify');
    expect(bookingChip(booking({ status: 'EXPIRED' }))).toBe('expired');
    const b = booking({});
    expect(cancellable(b, new Date(new Date(b.startsAt).getTime() - HOUR))).toBe(true);
    expect(cancellable(b, new Date(new Date(b.startsAt).getTime() + HOUR))).toBe(false);
    expect(cancellable(booking({ status: 'CANCELLED' }), new Date(0))).toBe(false);
  });

  it('labels opening hours', () => {
    expect(hoursLabel({ day: 'SUNDAY', openTime: '06:00:00', closeTime: '22:30:00', closed: false })).toBe('6 AM – 10:30 PM');
    expect(hoursLabel({ day: 'SUNDAY', openTime: null, closeTime: null, closed: true })).toBe('Closed');
    expect(hoursLabel(undefined)).toBe('Closed');
  });
});
