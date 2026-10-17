import { ChangeDetectionStrategy, Component, computed, effect, inject, input, OnDestroy, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CircleCheck, Clock, LucideAngularModule, MapPin, Wallet } from 'lucide-angular';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { Booking, BookingPayments, PaymentClaim, VenueDetail } from '../../core/api.types';
import { bookingChip, cancellable, cancelOutcome } from '../../shared/booking-model';
import { clockAt, dayLabel, localClock, methodLabel, rupees } from '../../shared/format';
import { PayForm } from '../../shared/pay-form';
import { StatusChip } from '../../shared/status-chip';

/** One booking: when and where, paying the advance, the money so far, and cancelling. */
@Component({
  selector: 'fs-booking',
  imports: [FormsModule, RouterLink, LucideAngularModule, PayForm, StatusChip],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './booking.html',
})
export class BookingPage implements OnDestroy {
  private readonly api = inject(Api);

  readonly id = input.required<string>();

  /** Set when the player has just booked it. */
  readonly booked = input<string | undefined>();

  protected readonly icons = { CircleCheck, Clock, MapPin, Wallet };

  protected readonly fmt = { rupees, localClock, dayLabel, methodLabel, clockAt };

  protected readonly booking = signal<Booking | null>(null);

  protected readonly payments = signal<BookingPayments | null>(null);

  protected readonly venue = signal<VenueDetail | null>(null);

  protected readonly error = signal<string | null>(null);

  protected readonly payError = signal<string | null>(null);

  protected readonly paying = signal(false);

  protected readonly cancelling = signal(false);

  protected readonly cancelOpen = signal(false);

  protected readonly cancelError = signal<string | null>(null);

  protected readonly reason = signal('');

  /** Ticks every 30 seconds, so the time left to pay and the cancel fee stay current. */
  protected readonly now = signal(new Date());

  private readonly timer = setInterval(() => this.now.set(new Date()), 30_000);

  /** A payment the venue hasn't checked yet. */
  protected readonly checking = computed(() => this.payments()?.payments.some((p) => p.status === 'PENDING') ?? false);

  protected readonly chip = computed(() => {
    const booking = this.booking();
    return booking ? bookingChip(booking, this.checking()) : 'confirmed';
  });

  protected readonly canPay = computed(() => {
    const booking = this.booking();
    return booking?.status === 'PENDING_PAYMENT' && !this.checking() && booking.holdExpiresAt !== null
      && new Date(booking.holdExpiresAt).getTime() > this.now().getTime();
  });

  protected readonly minutesLeft = computed(() => {
    const until = this.booking()?.holdExpiresAt;
    return until ? Math.max(0, Math.ceil((new Date(until).getTime() - this.now().getTime()) / 60_000)) : 0;
  });

  protected readonly canCancel = computed(() => {
    const booking = this.booking();
    return booking ? cancellable(booking, this.now()) : false;
  });

  protected readonly outcome = computed(() => {
    const booking = this.booking();
    const venue = this.venue();
    return booking && venue ? cancelOutcome(booking, venue.policy, this.now()) : null;
  });

  /** What's left to pay at the venue. */
  protected readonly atVenue = computed(() => {
    const booking = this.booking();
    return booking ? Math.max(0, booking.totalAmount - booking.paidAmount) : 0;
  });

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => void this.load(id));
    });
  }

  ngOnDestroy(): void {
    clearInterval(this.timer);
  }

  protected async pay(claim: PaymentClaim): Promise<void> {
    const booking = this.booking();
    if (!booking || this.paying()) {
      return;
    }
    this.paying.set(true);
    this.payError.set(null);
    try {
      await this.api.payBooking(booking.id, claim);
      await this.load(booking.id);
    }
    catch (error) {
      this.payError.set(problemOf(error).message);
    }
    finally {
      this.paying.set(false);
    }
  }

  protected async cancel(): Promise<void> {
    const booking = this.booking();
    if (!booking || this.cancelling()) {
      return;
    }
    this.cancelling.set(true);
    this.cancelError.set(null);
    try {
      this.booking.set(await this.api.cancel(booking.id, this.reason().trim()));
      this.cancelOpen.set(false);
      this.payments.set(await this.api.bookingPayments(booking.id));
    }
    catch (error) {
      this.cancelError.set(problemOf(error).message);
    }
    finally {
      this.cancelling.set(false);
    }
  }

  private async load(id: string): Promise<void> {
    this.error.set(null);
    try {
      const [booking, payments] = await Promise.all([this.api.booking(id), this.api.bookingPayments(id)]);
      this.booking.set(booking);
      this.payments.set(payments);
      this.now.set(new Date());
      if (this.venue()?.id !== booking.venue.id) {
        this.venue.set(await this.api.venue(booking.venue.slug));
      }
    }
    catch (error) {
      if (!this.booking()) {
        this.error.set(problemOf(error).message);
      }
    }
  }
}
