import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Check, LoaderCircle, LucideAngularModule, Minus, Phone, Plus, X } from 'lucide-angular';

import { problemOf } from '../../core/api-error';
import { AddOnItem, Booking, Payment, PaymentMethod } from '../../core/api.types';
import { localClock, methodLabel, phone, rupees } from '../../shared/format';
import { Sheet } from '../../shared/sheet';
import { StatusChip } from '../../shared/status-chip';
import { ScheduleApi } from './schedule-api';
import { stateOf } from './schedule-model';

type Step = 'view' | 'take-payment' | 'reject' | 'cancel' | 'extras';

const SOURCES: Record<string, string> = {
  APP: 'Booked in the app',
  WEBSITE: 'Booked on your website',
  WALK_IN: 'Walk-in',
  PHONE: 'Phone booking',
  RECURRING: 'Weekly booking',
  TOURNAMENT: 'Tournament',
  ADMIN: 'Added by FutSlot',
};

const METHODS: { value: PaymentMethod; label: string }[] = (['CASH', 'ESEWA', 'KHALTI', 'FONEPAY', 'BANK_TRANSFER'] as const)
  .map((value) => ({ value, label: value === 'BANK_TRANSFER' ? 'Bank' : methodLabel(value) }));

/**
 * One booking: who, when, what's paid, and the next thing staff are likely to do with it (verify a
 * payment, take money, mark it played). Rarer actions sit lower and ask before doing anything final.
 */
@Component({
  selector: 'fs-booking-panel',
  imports: [FormsModule, LucideAngularModule, Sheet, StatusChip],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './booking-panel.html',
})
export class BookingPanel implements OnInit {
  private readonly api = inject(ScheduleApi);

  readonly venueId = input.required<string>();

  readonly booking = input.required<Booking>();

  readonly canManage = input(false);

  readonly done = output<void>();

  readonly closed = output<void>();

  protected readonly icons = { Check, LoaderCircle, Minus, Phone, Plus, X };

  protected readonly rupees = rupees;

  protected readonly methodLabel = methodLabel;

  protected readonly phone = phone;

  protected readonly methods = METHODS;

  protected readonly step = signal<Step>('view');

  protected readonly busy = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly pending = signal<Payment | null>(null);

  protected readonly amount = signal(0);

  protected readonly method = signal<PaymentMethod>('CASH');

  protected readonly transactionId = signal('');

  protected readonly reason = signal('');

  /** The venue's extras on sale, with how many of each are on this booking. */
  protected readonly extras = signal<{ item: AddOnItem; quantity: number }[] | null>(null);

  protected readonly extrasTotal = computed(() =>
    (this.extras() ?? []).reduce((sum, line) => sum + line.item.price * line.quantity, 0));

  protected readonly state = computed(() => stateOf(this.booking()));

  protected readonly due = computed(() => Math.max(0, this.booking().totalAmount - this.booking().paidAmount));

  protected readonly when = computed(() =>
    `${this.booking().court.name}, ${localClock(this.booking().startsAt)}–${localClock(this.booking().endsAt)}`);

  protected readonly source = computed(() => SOURCES[this.booking().source] ?? this.booking().source);

  protected readonly live = computed(() => ['PENDING_PAYMENT', 'CONFIRMED'].includes(this.booking().status));

  protected readonly started = computed(() => new Date(this.booking().startsAt) <= new Date());

  protected readonly canEditExtras = computed(() =>
    this.canManage() && ['PENDING_PAYMENT', 'CONFIRMED', 'COMPLETED'].includes(this.booking().status));

  protected readonly canTakePayment = computed(() =>
    this.canManage() && this.due() > 0 && this.state() !== 'verify'
      && ['PENDING_PAYMENT', 'CONFIRMED', 'COMPLETED'].includes(this.booking().status));

  async ngOnInit(): Promise<void> {
    this.amount.set(this.due());
    if (this.state() === 'verify' && this.canManage()) {
      try {
        const pending = await this.api.pendingPayments(this.venueId());
        this.pending.set(pending.find((payment) => payment.bookingId === this.booking().id) ?? null);
      }
      catch (error) {
        this.error.set(problemOf(error).message);
      }
    }
  }

  protected approve(): Promise<void> {
    return this.run(() => this.api.approve(this.venueId(), this.pending()!.id));
  }

  protected reject(): Promise<void> {
    if (this.reason().trim().length < 3) {
      this.error.set('Say why, so the player knows what to fix.');
      return Promise.resolve();
    }
    return this.run(() => this.api.reject(this.venueId(), this.pending()!.id, this.reason().trim()));
  }

  protected takePayment(): Promise<void> {
    if (!(this.amount() > 0) || this.amount() > this.due()) {
      this.error.set(`Enter an amount up to ${rupees(this.due())}.`);
      return Promise.resolve();
    }
    return this.run(() => this.api.takePayment(this.venueId(), this.booking().id, this.amount(), this.method(),
      this.transactionId().trim() || undefined));
  }

  protected played(): Promise<void> {
    return this.run(() => this.api.played(this.venueId(), this.booking().id));
  }

  protected noShow(): Promise<void> {
    return this.run(() => this.api.noShow(this.venueId(), this.booking().id));
  }

  protected cancel(): Promise<void> {
    return this.run(() => this.api.cancel(this.venueId(), this.booking().id, this.reason().trim()));
  }

  protected async openExtras(): Promise<void> {
    this.go('extras');
    try {
      const items = await this.api.addOns(this.venueId());
      const onBooking = new Map(this.booking().addOns.map((line) => [line.itemId, line.quantity]));
      this.extras.set(items
        .filter((item) => item.active || onBooking.has(item.id))
        .map((item) => ({ item, quantity: onBooking.get(item.id) ?? 0 })));
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }

  protected adjust(itemId: string, change: number): void {
    this.extras.update((lines) => (lines ?? []).map((line) =>
      line.item.id === itemId ? { ...line, quantity: Math.max(0, Math.min(50, line.quantity + change)) } : line));
  }

  protected saveExtras(): Promise<void> {
    const lines = (this.extras() ?? [])
      .filter((line) => line.quantity > 0)
      .map((line) => ({ itemId: line.item.id, quantity: line.quantity }));
    return this.run(() => this.api.setAddOns(this.venueId(), this.booking().id, lines));
  }

  protected go(step: Step): void {
    this.error.set(null);
    this.reason.set('');
    this.step.set(step);
  }

  private async run(action: () => Promise<unknown>): Promise<void> {
    if (this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      await action();
      this.done.emit();
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }
}
