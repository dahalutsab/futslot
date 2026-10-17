import { Availability, Booking, CourtBlock } from '../../core/api.types';
import { addDays, clock, minutesInto, rupees } from '../../shared/format';
import { buildDay, stateOf } from './schedule-model';

const DATE = '2026-10-09';

function slot(hour: number, status: 'AVAILABLE' | 'BOOKED' | 'BLOCKED' | 'PAST') {
  const hh = (h: number) => String(h % 24).padStart(2, '0');
  const day = (h: number) => (h >= 24 ? addDays(DATE, 1) : DATE);
  return {
    startsAt: `${day(hour)}T${hh(hour)}:00:00+05:45`,
    endsAt: `${day(hour + 1)}T${hh(hour + 1)}:00:00+05:45`,
    price: 1200,
    label: null,
    status,
  };
}

function booking(overrides: Partial<Booking>): Booking {
  return {
    id: 'b1', reference: 'FS-ABC123', venue: { id: 'v', name: 'V', slug: 'v' }, court: { id: 'c1', name: 'Court A' },
    customer: { id: 'cu', name: 'Ram', phone: '+9779800000000' }, source: 'APP', status: 'CONFIRMED',
    startsAt: `${DATE}T18:00:00+05:45`, endsAt: `${DATE}T19:00:00+05:45`, priceAmount: 1200, addOnsAmount: 0,
    addOns: [], discountAmount: 0, membershipId: null, discountCodeId: null, totalAmount: 1200,
    advanceRequired: 0, paidAmount: 0, paymentStatus: 'UNPAID', holdExpiresAt: null, notes: null,
    cancelledAt: null, cancellationReason: null, cancellationFeePercent: 0, createdAt: '2026-10-01T00:00:00Z', ...overrides,
  };
}

describe('schedule model', () => {
  const availability: Availability = {
    venueId: 'v', date: DATE, timezone: 'Asia/Kathmandu',
    courts: [{ courtId: 'c1', name: 'Court A', format: 'FIVE_A_SIDE', slotDurationMinutes: 60,
      slots: [slot(6, 'PAST'), slot(7, 'AVAILABLE'), slot(18, 'BOOKED'), slot(19, 'BLOCKED'), slot(24, 'AVAILABLE')] }],
  };

  it('places slots, bookings and blocks in minutes from the start of the day', () => {
    const block: CourtBlock = { id: 'k', courtId: 'c1', startsAt: `${DATE}T13:15:00Z`, endsAt: `${DATE}T14:15:00Z`,
      reason: 'TOURNAMENT', note: 'Cup: A vs B' };
    const day = buildDay(availability, [booking({}), booking({ id: 'gone', status: 'CANCELLED' })], [block],
      new Date(`${DATE}T00:00:00Z`));

    const items = day.courts[0].items;
    expect(items.map((item) => [item.kind, item.start])).toEqual([
      ['past', 360], ['free', 420], ['booking', 1080], ['block', 1140], ['free', 1440],
    ]);
    expect(items[3]).toMatchObject({ tournament: true });
    expect(day.start).toBe(360);
    expect(day.end).toBe(1500);
    expect(day.summary).toMatchObject({ bookings: 1, bookedValue: 1200, freeSlotsLeft: 2 });
    expect(day.summary.next?.id).toBe('b1');
  });

  it('tells a held booking from one whose payment is being checked', () => {
    expect(stateOf(booking({ status: 'PENDING_PAYMENT', holdExpiresAt: '2026-10-09T12:00:00Z' }))).toBe('held');
    expect(stateOf(booking({ status: 'PENDING_PAYMENT', holdExpiresAt: null }))).toBe('verify');
    expect(stateOf(booking({ status: 'NO_SHOW' }))).toBe('noshow');
    expect(stateOf(booking({ status: 'EXPIRED' }))).toBeNull();
  });

  it('formats money and times the way Nepal reads them', () => {
    expect(rupees(120000)).toBe('Rs 1,20,000');
    expect(clock(18 * 60)).toBe('6 PM');
    expect(clock(6 * 60 + 30)).toBe('6:30 AM');
    expect(clock(24 * 60)).toBe('12 AM');
    expect(minutesInto(DATE, `${addDays(DATE, 1)}T00:30:00+05:45`, 'Asia/Kathmandu')).toBe(1470);
  });
});
