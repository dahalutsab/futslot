import { Court, DayHours, PriceLabel, PricingRule, Weekday } from '../../core/api.types';
import { addDays, clock, dayLabel } from '../../shared/format';

const WEEKDAYS: Weekday[] = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

export const LABELS: Record<PriceLabel, string> = {
  STANDARD: 'Standard',
  PEAK: 'Peak',
  OFF_PEAK: 'Off-peak',
  WEEKEND: 'Weekend',
  HOLIDAY: 'Holiday',
};

export function weekdayOf(date: string): Weekday {
  const [y, m, d] = date.split('-').map(Number);
  return WEEKDAYS[new Date(Date.UTC(y, m - 1, d)).getUTCDay()];
}

/** "HH:mm[:ss]" to minutes past midnight. */
export function minutesOf(time: string): number {
  const [h, m] = time.split(':').map(Number);
  return h * 60 + m;
}

/** The rule that prices a slot: highest priority, then a specific day over every day, then the newest. */
export function ruleFor(rules: PricingRule[], date: string, slotStart: number): PricingRule | null {
  const weekday = weekdayOf(date);
  const time = slotStart % 1440;
  let best: PricingRule | null = null;
  for (const rule of rules) {
    const applies = rule.active
      && (rule.dayOfWeek === null || rule.dayOfWeek === weekday)
      && (rule.validFrom === null || date >= rule.validFrom)
      && (rule.validTo === null || date <= rule.validTo)
      && time >= minutesOf(rule.startTime) && time < minutesOf(rule.endTime);
    if (applies && (!best || outranks(rule, best))) {
      best = rule;
    }
  }
  return best;
}

function outranks(a: PricingRule, b: PricingRule): boolean {
  if (a.priority !== b.priority) {
    return a.priority > b.priority;
  }
  if ((a.dayOfWeek !== null) !== (b.dayOfWeek !== null)) {
    return a.dayOfWeek !== null;
  }
  return a.id > b.id;
}

export interface PriceCell {
  /** Minutes from the operating day's midnight; past 1440 for slots after midnight. */
  start: number;
  rule: PricingRule | null;
}

export interface PriceDay {
  date: string;
  label: string;
  closed: boolean;
  cells: PriceCell[];
}

/** The slots a court offers each day from {@code from}, as the API works them out, with the price for each. */
export function priceWeek(court: Court, hours: DayHours[], from: string, days = 7): PriceDay[] {
  return Array.from({ length: days }, (_, i) => {
    const date = addDays(from, i);
    const day = hours.find((h) => h.day === weekdayOf(date));
    if (!day || day.closed || !day.openTime || !day.closeTime) {
      return { date, label: dayLabel(date), closed: true, cells: [] };
    }
    const open = minutesOf(day.openTime);
    let close = minutesOf(day.closeTime);
    if (close <= open) {
      close += 1440;
    }
    const cells: PriceCell[] = [];
    for (let start = open; start + court.slotDurationMinutes <= close; start += court.slotDurationMinutes) {
      cells.push({ start, rule: ruleFor(court.pricingRules, date, start) });
    }
    return { date, label: dayLabel(date), closed: false, cells };
  });
}

export interface Gap {
  date: string;
  label: string;
  weekday: Weekday;
  from: number;
  to: number;
}

/** Runs of open slots with no price, which players can't book. */
export function gaps(week: PriceDay[], slotMinutes: number): Gap[] {
  const found: Gap[] = [];
  for (const day of week) {
    let current: Gap | null = null;
    for (const cell of day.cells) {
      if (cell.rule) {
        current = null;
      } else if (current && current.to === cell.start) {
        current.to = cell.start + slotMinutes;
      } else {
        current = { date: day.date, label: day.label, weekday: weekdayOf(day.date), from: cell.start, to: cell.start + slotMinutes };
        found.push(current);
      }
    }
  }
  return found;
}

/** "10 PM – 1 AM". */
export function span(from: number, to: number): string {
  return `${clock(from)} – ${clock(to)}`;
}

/** A rule's window as people say it; an end at the last minute of the day reads as midnight. */
export function ruleWindow(rule: Pick<PricingRule, 'startTime' | 'endTime'>): string {
  const end = minutesOf(rule.endTime) >= 1439 ? 1440 : minutesOf(rule.endTime);
  return span(minutesOf(rule.startTime), end);
}

export interface RuleWindow {
  startTime: string;
  endTime: string;
}

/**
 * The API's windows for a start and end typed as "HH:mm". Rules can't cross midnight, so an end at or before
 * the start (late-night prices) becomes two windows: to midnight, and from midnight, both on the same day.
 */
export function windows(start: string, end: string): RuleWindow[] {
  const toMidnight = { startTime: `${start}:00`, endTime: '23:59:59' };
  if (end === '00:00') {
    return [toMidnight];
  }
  if (end > start) {
    return [{ startTime: `${start}:00`, endTime: `${end}:00` }];
  }
  return [toMidnight, { startTime: '00:00:00', endTime: `${end}:00` }];
}
