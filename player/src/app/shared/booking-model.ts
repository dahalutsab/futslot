import { Booking, CancellationTier, DayHours, DEPOSIT_AMOUNT, STRIKE_WINDOW_DAYS, VenuePolicy, Weekday } from '../core/api.types';
import { ChipState } from './status-chip';
import { clock, rupees } from './format';

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

/** Mirrors the server: the fee of the closest tier the notice falls inside, 0 at 24 hours or more. */
export function feePercent(tiers: CancellationTier[], noticeMs: number): number {
  let fee = 0;
  for (const tier of byHoursBefore(tiers)) {
    if (noticeMs < tier.withinHours * 3_600_000) {
      fee = tier.feePercent;
    }
  }
  return fee;
}

export interface CancelOutcome {
  feePercent: number;
  /** The fee in rupees: a share of the booking price. */
  fee: number;
  /** What the venue keeps of what was paid. */
  kept: number;
  /** What comes back. */
  refund: number;
  /** What the deposit pays on top, at most (only for bookings it backs). */
  fromDeposit: number;
}

/** What cancelling {@code booking} at {@code now} costs, the way the server works it out. */
export function cancelOutcome(booking: Booking, policy: VenuePolicy, now = new Date()): CancelOutcome {
  const percent = feePercent(policy.cancellationTiers, new Date(booking.startsAt).getTime() - now.getTime());
  const fee = Math.round(booking.totalAmount * percent) / 100;
  const kept = Math.min(fee, booking.paidAmount);
  return {
    feePercent: percent,
    fee,
    kept,
    refund: booking.paidAmount - kept,
    fromDeposit: booking.depositBacked ? Math.min(fee - kept, DEPOSIT_AMOUNT) : 0,
  };
}

/** The venue's booking rules, said to the player. */
export function policyLines(policy: VenuePolicy): string[] {
  const lines: string[] = [];
  lines.push(policy.advancePercent === 0
    ? 'Book now and pay at the venue.'
    : `Pay ${policy.advancePercent}% up front to confirm. We hold the slot for ${policy.bookingHoldMinutes} minutes while you pay.`);
  const tiers = byHoursBefore(policy.cancellationTiers);
  if (tiers.length === 0) {
    lines.push('Cancel any time before the game for free.');
  } else {
    const fees = tiers.map((tier) => `${tier.feePercent}% within ${tier.withinHours} ${hours(tier.withinHours)}`);
    const listed = fees.length === 1 ? fees[0] : `${fees.slice(0, -1).join(', ')} and ${fees[fees.length - 1]}`;
    lines.push(`Cancel ${tiers[0].withinHours} ${hours(tiers[0].withinHours)} or more before the game for free.`);
    lines.push(`Cancelling later costs a share of the booking price: ${listed}. ${policy.depositRequired
      ? 'What you paid covers it first; your FutSlot deposit covers the rest.'
      : "It's never more than you've paid."}`);
  }
  if (policy.depositRequired) {
    const top = Math.max(0, ...tiers.map((tier) => tier.feePercent));
    lines.push(`You need a ${rupees(DEPOSIT_AMOUNT)} FutSlot booking deposit to book here.${top > 0
      ? ` Not showing up costs ${top}% of the booking price.` : ''}`);
  }
  if (policy.strikeLimit !== null && policy.strikeAdvancePercent !== null && policy.strikeAdvancePercent > policy.advancePercent) {
    const strikes = policy.strikeLimit === 1 ? 'a late cancellation or no-show' : `${policy.strikeLimit} late cancellations or no-shows`;
    lines.push(`After ${strikes} here in ${STRIKE_WINDOW_DAYS} days, you pay ${policy.strikeAdvancePercent}% up front.`);
  }
  lines.push('If the venue cancels, you get everything back.');
  return lines;
}

/** How a booking shows in lists. {@code checking}: a payment for it is waiting to be checked. */
export function bookingChip(booking: Booking, checking = false): ChipState {
  switch (booking.status) {
    case 'PENDING_PAYMENT':
      return checking ? 'verify' : 'held';
    case 'CONFIRMED':
      return 'confirmed';
    case 'COMPLETED':
      return 'completed';
    case 'NO_SHOW':
      return 'noshow';
    case 'CANCELLED':
      return 'cancelled';
    case 'EXPIRED':
      return 'expired';
  }
}

/** Whether the player can still cancel it themselves: live and not started. */
export function cancellable(booking: Booking, now = new Date()): boolean {
  return (booking.status === 'PENDING_PAYMENT' || booking.status === 'CONFIRMED')
    && new Date(booking.startsAt).getTime() > now.getTime();
}

/** "6 AM – 10 PM", "Closed", for one day's opening hours. */
export function hoursLabel(hours: DayHours | undefined): string {
  if (!hours || hours.closed || !hours.openTime || !hours.closeTime) {
    return 'Closed';
  }
  return `${timeLabel(hours.openTime)} – ${timeLabel(hours.closeTime)}`;
}

/** "6 AM", "6:30 PM" from "HH:mm:ss". */
export function timeLabel(time: string): string {
  const [h, m] = time.split(':').map(Number);
  return clock(h * 60 + m);
}

function byHoursBefore(tiers: CancellationTier[]): CancellationTier[] {
  return [...tiers].sort((a, b) => b.withinHours - a.withinHours);
}

function hours(count: number): string {
  return count === 1 ? 'hour' : 'hours';
}
