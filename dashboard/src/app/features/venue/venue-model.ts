import { DayHours, VenuePolicy, Weekday } from '../../core/api.types';

/** The week as people in Nepal count it, Sunday first. */
export const WEEK: { day: Weekday; label: string; short: string }[] = [
  { day: 'SUNDAY', label: 'Sunday', short: 'Sun' },
  { day: 'MONDAY', label: 'Monday', short: 'Mon' },
  { day: 'TUESDAY', label: 'Tuesday', short: 'Tue' },
  { day: 'WEDNESDAY', label: 'Wednesday', short: 'Wed' },
  { day: 'THURSDAY', label: 'Thursday', short: 'Thu' },
  { day: 'FRIDAY', label: 'Friday', short: 'Fri' },
  { day: 'SATURDAY', label: 'Saturday', short: 'Sat' },
];

export interface HoursRow {
  day: Weekday;
  label: string;
  open: boolean;
  /** "HH:mm", as an <input type="time"> uses. */
  from: string;
  to: string;
}

/** One editable row per weekday, Sunday first; days never set default to 6 AM–10 PM, open. */
export function hoursRows(hours: DayHours[]): HoursRow[] {
  return WEEK.map(({ day, label }) => {
    const saved = hours.find((h) => h.day === day);
    // A closed day's stored times mean nothing; reopening it starts from the suggested hours
    const open = saved && !saved.closed ? saved : null;
    return {
      day,
      label,
      open: saved ? !saved.closed : true,
      from: open?.openTime?.slice(0, 5) ?? '06:00',
      to: open?.closeTime?.slice(0, 5) ?? '22:00',
    };
  });
}

export function toDayHours(rows: HoursRow[]): DayHours[] {
  return rows.map((row) => (row.open
    ? { day: row.day, openTime: row.from, closeTime: row.to, closed: false }
    : { day: row.day, openTime: null, closeTime: null, closed: true }));
}

/** Closing at or before opening means the venue stays open past midnight. */
export function overnight(row: HoursRow): boolean {
  return row.open && !!row.from && !!row.to && row.to <= row.from;
}

/** The first problem with the rows, in words, or null. */
export function hoursProblem(rows: HoursRow[]): string | null {
  const bad = rows.find((row) => row.open && (!row.from || !row.to));
  if (bad) {
    return `Set both times for ${bad.label}, or mark it closed.`;
  }
  const same = rows.find((row) => row.open && row.from === row.to);
  return same ? `${same.label} opens and closes at the same time.` : null;
}

/**
 * A map location from what people paste: "27.7172, 85.3240" or a Google Maps link ("…/@27.7172,85.324,17z"
 * or "…?q=27.7172,85.324"). Null when there are no usable coordinates.
 */
export function parseLocation(text: string): { latitude: number; longitude: number } | null {
  const match = text.match(/@(-?\d+(?:\.\d+)?),\s*(-?\d+(?:\.\d+)?)/)
    ?? text.match(/[?&](?:q|query|ll)=(-?\d+(?:\.\d+)?),\s*(-?\d+(?:\.\d+)?)/)
    ?? text.trim().match(/^(-?\d+(?:\.\d+)?)\s*[,\s]\s*(-?\d+(?:\.\d+)?)$/);
  if (!match) {
    return null;
  }
  const latitude = Number(match[1]);
  const longitude = Number(match[2]);
  if (Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
    return null;
  }
  return { latitude: Number(latitude.toFixed(6)), longitude: Number(longitude.toFixed(6)) };
}

/** The booking rules as a player would hear them. */
export function policySummary(policy: VenuePolicy): string[] {
  const lines: string[] = [];
  lines.push(policy.advancePercent === 0
    ? 'Players book without paying anything up front and pay at the venue.'
    : `Players pay ${policy.advancePercent}% up front to confirm. Unpaid bookings are released after ${policy.bookingHoldMinutes} minutes.`);
  const late = policy.lateCancelRefundPct === 0 ? 'nothing back' : `${policy.lateCancelRefundPct}% back`;
  lines.push(policy.freeCancelHours === 0
    ? 'Cancelling any time before the game gets a full refund.'
    : `Cancelling ${policy.freeCancelHours} hours or more before the game gets a full refund; later gets ${late}.`);
  lines.push('If you cancel, players always get everything back.');
  lines.push(`Players can book up to ${policy.maxAdvanceBookingDays} days ahead.`);
  return lines;
}
