import { Availability, Booking, CourtBlock, Slot } from '../../core/api.types';
import { minutesInto } from '../../shared/format';

/** How a booking reads on the schedule; each has one colour across the dashboard. */
export type BookingState = 'confirmed' | 'held' | 'verify' | 'completed' | 'noshow';

export type ScheduleItem =
  | { kind: 'free'; start: number; end: number; slot: Slot }
  | { kind: 'past'; start: number; end: number; slot: Slot }
  | { kind: 'booking'; start: number; end: number; booking: Booking; state: BookingState }
  | { kind: 'block'; start: number; end: number; block: CourtBlock; tournament: boolean };

export interface CourtColumn {
  id: string;
  name: string;
  items: ScheduleItem[];
}

export interface DayModel {
  date: string;
  timeZone: string;
  /** The window shown, in minutes from the date's midnight, rounded out to whole hours. */
  start: number;
  end: number;
  courts: CourtColumn[];
  summary: DaySummary;
}

export interface DaySummary {
  /** Bookings that are on or were played (not cancelled or lapsed). */
  bookings: number;
  bookedValue: number;
  collected: number;
  toVerify: number;
  freeSlotsLeft: number;
  /** Next booking yet to start, if the day is today. */
  next: Booking | null;
}

/**
 * A booking waiting for payment whose hold is paused has a payment being checked by staff; one still on
 * the clock is just held.
 */
export function stateOf(booking: Booking): BookingState | null {
  switch (booking.status) {
    case 'PENDING_PAYMENT':
      return booking.holdExpiresAt === null ? 'verify' : 'held';
    case 'CONFIRMED':
      return 'confirmed';
    case 'COMPLETED':
      return 'completed';
    case 'NO_SHOW':
      return 'noshow';
    default:
      return null;
  }
}

/**
 * Lays the day out per court. Free and past slots come from availability; bookings and blocks sit on top
 * at their own times, so a booking that doesn't match the slot grid (a manual 90-minute booking) still
 * shows where it really is.
 */
export function buildDay(availability: Availability, bookings: Booking[], blocks: CourtBlock[], now: Date): DayModel {
  const { date, timezone: timeZone } = availability;
  const at = (iso: string) => minutesInto(date, iso, timeZone);
  const nowMinutes = minutesInto(date, now.toISOString(), timeZone);

  const live = bookings
    .map((booking) => ({ booking, state: stateOf(booking) }))
    .filter((entry): entry is { booking: Booking; state: BookingState } => entry.state !== null);

  const courts: CourtColumn[] = availability.courts.map((court) => {
    const items: ScheduleItem[] = [];
    for (const slot of court.slots) {
      if (slot.status === 'AVAILABLE') {
        items.push({ kind: 'free', start: at(slot.startsAt), end: at(slot.endsAt), slot });
      }
      else if (slot.status === 'PAST') {
        items.push({ kind: 'past', start: at(slot.startsAt), end: at(slot.endsAt), slot });
      }
    }
    for (const { booking, state } of live.filter((entry) => entry.booking.court.id === court.courtId)) {
      items.push({ kind: 'booking', start: at(booking.startsAt), end: at(booking.endsAt), booking, state });
    }
    for (const block of blocks.filter((entry) => entry.courtId === court.courtId)) {
      const start = at(block.startsAt);
      const end = at(block.endsAt);
      // Blocks are fetched for a wider window; keep the ones in this operating day (up to 6 AM next morning)
      if (end > 0 && start < 30 * 60) {
        items.push({ kind: 'block', start, end, block, tournament: block.reason === 'TOURNAMENT' });
      }
    }
    items.sort((a, b) => a.start - b.start);
    return { id: court.courtId, name: court.name, items };
  });

  const all = courts.flatMap((court) => court.items);
  const start = all.length > 0 ? Math.floor(Math.min(...all.map((item) => item.start)) / 60) * 60 : 6 * 60;
  const end = all.length > 0 ? Math.ceil(Math.max(...all.map((item) => item.end)) / 60) * 60 : 22 * 60;

  const upcoming = live
    .filter((entry) => entry.state !== 'completed' && entry.state !== 'noshow' && at(entry.booking.startsAt) > nowMinutes)
    .sort((a, b) => a.booking.startsAt.localeCompare(b.booking.startsAt));

  return {
    date,
    timeZone,
    start,
    end,
    courts,
    summary: {
      bookings: live.length,
      bookedValue: live.reduce((sum, entry) => sum + entry.booking.totalAmount, 0),
      collected: live.reduce((sum, entry) => sum + entry.booking.paidAmount, 0),
      toVerify: live.filter((entry) => entry.state === 'verify').length,
      freeSlotsLeft: all.filter((item) => item.kind === 'free').length,
      next: upcoming[0]?.booking ?? null,
    },
  };
}
