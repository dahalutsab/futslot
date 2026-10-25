import NepaliDate from 'nepali-date-converter';

import { digits, inNepali, l } from '../i18n/i18n';

/**
 * Display helpers, in the app's language (see {@link l}). Times are shown in the venue's time zone, 12-hour, the way
 * people in Nepal say them; in Nepali with Devanagari digits and Bikram Sambat dates. Phone numbers and transaction
 * ids stay in Latin digits, so they can be copied and typed.
 */

const AMOUNT = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 2 });

/**
 * "1,200", "1,20,000" (lakh grouping); "१,२००" in Nepali. The digits are swapped rather than formatted as 'ne',
 * because browsers differ on whether Nepali numbers default to Devanagari.
 */
export function amount(value: number): string {
  return digits(AMOUNT.format(value));
}

/** "Rs 1,200", "Rs 1,20,000" (lakh grouping); "रु १,२००" in Nepali. */
export function rupees(value: number | null | undefined): string {
  return `${inNepali() ? 'रु' : 'Rs'} ${amount(value ?? 0)}`;
}

/**
 * "18:00", "06:30" from minutes past midnight (values past 1440 roll into the next day), on the 24-hour clock as
 * Broadsheet sets times. In Nepali the part of the day comes first, as it's said: "बिहान ६ बजे", "बेलुका ६:३० बजे".
 */
export function clock(minutes: number): string {
  const inDay = ((minutes % 1440) + 1440) % 1440;
  const hours = Math.floor(inDay / 60);
  const mins = inDay % 60;
  const hour12 = hours % 12 === 0 ? 12 : hours % 12;
  const time = mins === 0 ? `${hour12}` : `${hour12}:${String(mins).padStart(2, '0')}`;
  if (inNepali()) {
    const part = hours >= 4 && hours < 12 ? 'बिहान' : hours >= 12 && hours < 16 ? 'दिउँसो' : hours >= 16 && hours < 20 ? 'बेलुका' : 'राति';
    return `${part} ${digits(time)} बजे`;
  }
  return `${String(hours).padStart(2, '0')}:${String(mins).padStart(2, '0')}`;
}

/** "18:00" from a venue-local timestamp such as 2026-10-09T18:00:00+05:45, read as written (no zone maths). */
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

const BS_MONTHS = ['बैशाख', 'जेष्ठ', 'आषाढ', 'श्रावण', 'भाद्र', 'आश्विन', 'कार्तिक', 'मंसिर', 'पौष', 'माघ', 'फाल्गुण', 'चैत्र'];

const BS_WEEKDAYS = ['आइत', 'सोम', 'मंगल', 'बुध', 'बिहि', 'शुक्र', 'शनि'];

/** A YYYY-MM-DD in Bikram Sambat. The table behind it ends in 2090 BS (2034); later days stay in the Gregorian calendar. */
function bs(day: string): { year: number; month: string; date: number; weekday: string } | null {
  const [y, m, d] = day.split('-').map(Number);
  try {
    const date = new NepaliDate(new Date(y, m - 1, d, 12));
    return { year: date.getYear(), month: BS_MONTHS[date.getMonth()], date: date.getDate(), weekday: BS_WEEKDAYS[date.getDay()] };
  } catch {
    return null;
  }
}

function gregorian(day: string, options: Intl.DateTimeFormatOptions): string {
  const [y, m, d] = day.split('-').map(Number);
  return new Intl.DateTimeFormat(inNepali() ? 'ne' : 'en-GB', { ...options, timeZone: 'UTC' }).format(new Date(Date.UTC(y, m - 1, d)));
}

/** "Thu 9 Oct" for a YYYY-MM-DD; "बिहि, २३ आश्विन" (Bikram Sambat) in Nepali. */
export function dayLabel(day: string): string {
  const date = inNepali() ? bs(day) : null;
  return date ? `${date.weekday}, ${digits(date.date)} ${date.month}` : gregorian(day, { weekday: 'short', day: 'numeric', month: 'short' });
}

/** "Thu" for a YYYY-MM-DD; "बिहि" in Nepali. */
export function weekdayShort(day: string): string {
  const date = inNepali() ? bs(day) : null;
  return date ? date.weekday : gregorian(day, { weekday: 'short' });
}

/** "9 Oct" for a YYYY-MM-DD; "२३ आश्विन" in Nepali. */
export function shortDate(day: string): string {
  const date = inNepali() ? bs(day) : null;
  return date ? `${digits(date.date)} ${date.month}` : gregorian(day, { day: 'numeric', month: 'short' });
}

/** "Thu", "9", "Oct" for a YYYY-MM-DD, as on a calendar page; "बिहि", "२३", "आश्विन" in Nepali. */
export function calendarParts(day: string): { weekday: string; day: string; month: string } {
  const date = inNepali() ? bs(day) : null;
  if (date) {
    return { weekday: date.weekday, day: digits(date.date), month: date.month };
  }
  return { weekday: gregorian(day, { weekday: 'short' }), day: gregorian(day, { day: 'numeric' }), month: gregorian(day, { month: 'short' }) };
}

/** "Saturday 25 October 2026", for a dateline; "शनिबार, ८ कात्तिक २०८३" in Nepali. */
export function longDate(day: string): string {
  const date = inNepali() ? bs(day) : null;
  return date ? `${date.weekday}, ${digits(date.date)} ${date.month} ${digits(date.year)}`
    : gregorian(day, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).replace(',', '');
}

/** "9 Oct 2026" for a YYYY-MM-DD; "२३ आश्विन २०८३" in Nepali. */
export function fullDate(day: string): string {
  const date = inNepali() ? bs(day) : null;
  return date ? `${digits(date.date)} ${date.month} ${digits(date.year)}` : gregorian(day, { day: 'numeric', month: 'short', year: 'numeric' });
}

/** How each wallet spells its own name. */
export function methodLabel(method: string): string {
  switch (method) {
    case 'CASH':
      return l().methodCash;
    case 'ESEWA':
      return 'eSewa';
    case 'KHALTI':
      return 'Khalti';
    case 'FONEPAY':
      return 'Fonepay';
    case 'BANK_TRANSFER':
      return l().methodBankTransfer;
    case 'CARD':
      return l().methodCard;
    case 'MEMBERSHIP_CREDIT':
      return l().methodMembership;
    default:
      return method;
  }
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
    return l().justNow;
  }
  if (minutes < 60) {
    return l().minutesAgo(minutes);
  }
  if (minutes < 24 * 60) {
    return l().hoursAgo(Math.round(minutes / 60));
  }
  const then = new Date(instant);
  return shortDate(`${then.getFullYear()}-${String(then.getMonth() + 1).padStart(2, '0')}-${String(then.getDate()).padStart(2, '0')}`);
}
