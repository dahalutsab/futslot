import { hoursProblem, hoursRows, nextTier, overnight, parseLocation, policySummary, tiersProblem, toDayHours } from './venue-model';

describe('venue model', () => {
  it('orders the week from Sunday and fills days never set', () => {
    const rows = hoursRows([{ day: 'MONDAY', openTime: '07:00:00', closeTime: '21:30:00', closed: false },
      { day: 'SATURDAY', openTime: '00:00:00', closeTime: '00:00:00', closed: true }]);
    expect(rows.map((r) => r.label)).toEqual(['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']);
    expect(rows[1]).toMatchObject({ open: true, from: '07:00', to: '21:30' });
    expect(rows[6]).toMatchObject({ open: false, from: '06:00', to: '22:00' });
    expect(rows[0]).toMatchObject({ open: true, from: '06:00', to: '22:00' });
  });

  it('sends closed days without times', () => {
    const [sunday] = toDayHours([{ day: 'SUNDAY', label: 'Sunday', open: false, from: '06:00', to: '22:00' }]);
    expect(sunday).toEqual({ day: 'SUNDAY', openTime: null, closeTime: null, closed: true });
  });

  it('spots hours past midnight and impossible rows', () => {
    const late = { day: 'FRIDAY' as const, label: 'Friday', open: true, from: '18:00', to: '01:00' };
    expect(overnight(late)).toBe(true);
    expect(overnight({ ...late, to: '23:00' })).toBe(false);
    expect(hoursProblem([{ ...late, to: '' }])).toBe('Set both times for Friday, or mark it closed.');
    expect(hoursProblem([{ ...late, to: '18:00' }])).toBe('Friday opens and closes at the same time.');
    expect(hoursProblem([late])).toBeNull();
  });

  it('reads coordinates and Google Maps links', () => {
    expect(parseLocation('27.7172, 85.3240')).toEqual({ latitude: 27.7172, longitude: 85.324 });
    expect(parseLocation('https://www.google.com/maps/place/X/@27.6915196,85.3420486,17z/data')).toEqual({ latitude: 27.69152, longitude: 85.342049 });
    expect(parseLocation('https://maps.google.com/?q=27.7,85.3')).toEqual({ latitude: 27.7, longitude: 85.3 });
    expect(parseLocation('Baneshwor, Kathmandu')).toBeNull();
    expect(parseLocation('127, 85')).toBeNull();
  });

  it('describes the booking rules in plain words', () => {
    expect(policySummary({ advancePercent: 30, bookingHoldMinutes: 15, cancellationTiers: [{ withinHours: 24, feePercent: 100 }], maxAdvanceBookingDays: 30, strikeLimit: null, strikeAdvancePercent: null }))
      .toEqual(['Players pay 30% up front to confirm. Unpaid bookings are released after 15 minutes.',
        'Cancelling 24 hours or more before the game is free.',
        "Cancelling later costs a share of the booking price: 100% within 24 hours. It's never more than the player has paid.",
        'If you cancel, players always get everything back.',
        'Players can book up to 30 days ahead.']);
    expect(policySummary({ advancePercent: 0, bookingHoldMinutes: 15, cancellationTiers: [], maxAdvanceBookingDays: 7, strikeLimit: null, strikeAdvancePercent: null })[1])
      .toBe('Cancelling any time before the game is free.');
    expect(policySummary({ advancePercent: 0, bookingHoldMinutes: 15, cancellationTiers: [{ withinHours: 10, feePercent: 60 }, { withinHours: 24, feePercent: 20 }, { withinHours: 12, feePercent: 50 }], maxAdvanceBookingDays: 7, strikeLimit: null, strikeAdvancePercent: null })[2])
      .toBe("Cancelling later costs a share of the booking price: 20% within 24 hours, 50% within 12 hours and 60% within 10 hours. It's never more than the player has paid.");
  });

  it('mentions the strike rule only when it raises the advance', () => {
    const policy = { advancePercent: 0, bookingHoldMinutes: 15, cancellationTiers: [], maxAdvanceBookingDays: 7, strikeLimit: 2, strikeAdvancePercent: 50 };
    expect(policySummary(policy)).toContain('After 2 late cancellations or no-shows in 90 days, a player pays 50% up front to book.');
    expect(policySummary({ ...policy, strikeLimit: 1 })).toContain('After a late cancellation or no-show in 90 days, a player pays 50% up front to book.');
    expect(policySummary({ ...policy, advancePercent: 60 }).some((line) => line.startsWith('After'))).toBe(false);
  });

  it('checks cancellation fees the way the server does', () => {
    expect(tiersProblem([])).toBeNull();
    expect(tiersProblem([{ withinHours: 12, feePercent: 50 }, { withinHours: 24, feePercent: 20 }])).toBeNull();
    expect(tiersProblem([{ withinHours: 48, feePercent: 20 }])).toContain('1 to 24 hours');
    expect(tiersProblem([{ withinHours: 12, feePercent: 0 }])).toContain('1% to 100%');
    expect(tiersProblem([{ withinHours: 12, feePercent: 20 }, { withinHours: 12, feePercent: 50 }])).toContain('Two cancellation fees');
    expect(tiersProblem([{ withinHours: 24, feePercent: 50 }, { withinHours: 12, feePercent: 50 }])).toContain('must be more than');
  });

  it('suggests the next fee closer to the start', () => {
    expect(nextTier([])).toEqual({ withinHours: 24, feePercent: 50 });
    expect(nextTier([{ withinHours: 24, feePercent: 20 }, { withinHours: 12, feePercent: 50 }])).toEqual({ withinHours: 6, feePercent: 70 });
    expect(nextTier([{ withinHours: 1, feePercent: 90 }])).toEqual({ withinHours: 1, feePercent: 100 });
  });
});
