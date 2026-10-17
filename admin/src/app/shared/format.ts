/** Display helpers. Times are shown in the venue's time zone, 12-hour, the way people in Nepal say them. */

const RUPEES = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 2 });

/** "Rs 1,200", "Rs 1,20,000" (lakh grouping). */
export function rupees(amount: number | null | undefined): string {
  return `Rs ${RUPEES.format(amount ?? 0)}`;
}

/** "6 PM", "6:30 PM" from minutes past midnight (values past 1440 roll into the next day). */
export function clock(minutes: number): string {
  const inDay = ((minutes % 1440) + 1440) % 1440;
  const hours = Math.floor(inDay / 60);
  const mins = inDay % 60;
  const suffix = hours < 12 ? 'AM' : 'PM';
  const hour12 = hours % 12 === 0 ? 12 : hours % 12;
  return mins === 0 ? `${hour12} ${suffix}` : `${hour12}:${String(mins).padStart(2, '0')} ${suffix}`;
}

/** "6 PM" from a venue-local timestamp such as 2026-10-09T18:00:00+05:45, read as written (no zone maths). */
export function localClock(iso: string): string {
  const [hours, minutes] = iso.slice(11, 16).split(':').map(Number);
  return clock(hours * 60 + minutes);
}

/**
 * Minutes from midnight at the start of {@code day} (YYYY-MM-DD) to {@code instant}, in {@code timeZone}. A late
 * slot after midnight gives more than 1440.
 */
export function minutesInto(day: string, instant: string, timeZone: string): number {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(new Date(instant));
  const part = (type: string) => Number(parts.find((p) => p.type === type)?.value);
  const localDay = Date.UTC(part('year'), part('month') - 1, part('day'));
  const [y, m, d] = day.split('-').map(Number);
  const dayDiff = Math.round((localDay - Date.UTC(y, m - 1, d)) / 86_400_000);
  return dayDiff * 1440 + part('hour') * 60 + part('minute');
}

/** Today's date (YYYY-MM-DD) in {@code timeZone}. */
export function today(timeZone: string, now = new Date()): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit' }).format(now);
}

/** "6:05 PM" for an instant, as the clock reads in {@code timeZone}. */
export function clockAt(instant: string, timeZone: string): string {
  return clock(minutesInto(today(timeZone, new Date(instant)), instant, timeZone));
}

/** YYYY-MM-DD shifted by whole days. */
export function addDays(day: string, days: number): string {
  const [y, m, d] = day.split('-').map(Number);
  const date = new Date(Date.UTC(y, m - 1, d + days));
  return date.toISOString().slice(0, 10);
}

/** "Thu 9 Oct" for a YYYY-MM-DD. */
export function dayLabel(day: string): string {
  const [y, m, d] = day.split('-').map(Number);
  return new Intl.DateTimeFormat('en-GB', { weekday: 'short', day: 'numeric', month: 'short', timeZone: 'UTC' })
    .format(new Date(Date.UTC(y, m - 1, d)));
}

/** "September 2026" for a YYYY-MM-DD in that month. */
export function monthLabel(day: string): string {
  const [y, m] = day.split('-').map(Number);
  return new Intl.DateTimeFormat('en-GB', { month: 'long', year: 'numeric', timeZone: 'UTC' })
    .format(new Date(Date.UTC(y, m - 1, 1)));
}

const METHOD_LABELS: Record<string, string> = {
  CASH: 'Cash',
  ESEWA: 'eSewa',
  KHALTI: 'Khalti',
  FONEPAY: 'Fonepay',
  BANK_TRANSFER: 'Bank transfer',
  CARD: 'Card',
  MEMBERSHIP_CREDIT: 'Membership',
};

/** How each wallet spells its own name. */
export function methodLabel(method: string): string {
  return METHOD_LABELS[method] ?? method;
}

/** "+977 9856789012": the country code apart, so the familiar 10 digits stand out. */
export function phone(value: string | null | undefined): string {
  if (!value) {
    return '';
  }
  return value.startsWith('+977') && value.length === 14 ? `+977 ${value.slice(4)}` : value;
}

/** "just now", "5 min ago", "3 h ago", then "7 Oct". */
export function ago(instant: string, now = new Date()): string {
  const minutes = Math.round((now.getTime() - new Date(instant).getTime()) / 60_000);
  if (minutes < 1) {
    return 'just now';
  }
  if (minutes < 60) {
    return `${minutes} min ago`;
  }
  if (minutes < 24 * 60) {
    return `${Math.round(minutes / 60)} h ago`;
  }
  return new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short' }).format(new Date(instant));
}
