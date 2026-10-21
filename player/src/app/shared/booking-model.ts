import { Booking, CancellationTier, DayHours, DEPOSIT_AMOUNT, STRIKE_WINDOW_DAYS, VenuePolicy, Weekday } from '../core/api.types';
import { l } from '../i18n/i18n';
import { ChipState } from './status-chip';
import { clock, rupees } from './format';

/** The week as people in Nepal count it, Sunday first. */
export function week(): { day: Weekday; label: string }[] {
  return [
    { day: 'SUNDAY', label: l().sunday },
    { day: 'MONDAY', label: l().monday },
    { day: 'TUESDAY', label: l().tuesday },
    { day: 'WEDNESDAY', label: l().wednesday },
    { day: 'THURSDAY', label: l().thursday },
    { day: 'FRIDAY', label: l().friday },
    { day: 'SATURDAY', label: l().saturday },
  ];
}

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
  lines.push(policy.advancePercent === 0 ? l().policyPayAtVenue : l().policyAdvance(policy.advancePercent, policy.bookingHoldMinutes));
  const tiers = byHoursBefore(policy.cancellationTiers);
  if (tiers.length === 0) {
    lines.push(l().policyFreeCancelAnyTime);
  } else {
    const fees = tiers.map((tier) => l().feeWithin(tier.feePercent, l().hours(tier.withinHours)));
    lines.push(l().policyFreeBefore(l().hours(tiers[0].withinHours)));
    lines.push(l().policyLateFee(listOf(fees), policy.depositRequired ? l().policyFeeDeposit : l().policyFeeCapped));
  }
  if (policy.depositRequired) {
    const top = Math.max(0, ...tiers.map((tier) => tier.feePercent));
    lines.push([l().policyDeposit(rupees(DEPOSIT_AMOUNT)), ...(top > 0 ? [l().policyNoShow(top)] : [])].join(' '));
  }
  if (policy.strikeLimit !== null && policy.strikeAdvancePercent !== null && policy.strikeAdvancePercent > policy.advancePercent) {
    lines.push(l().policyStrikes(policy.strikeLimit, STRIKE_WINDOW_DAYS, policy.strikeAdvancePercent));
  }
  lines.push(l().policyVenueCancels);
  return lines;
}

/** "a", "a and b", "a, b and c". */
export function listOf(items: string[]): string {
  return items.length === 1 ? items[0] : l().listAnd(items.slice(0, -1).join(', '), items[items.length - 1]);
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
    return l().closed;
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
