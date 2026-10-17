import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LoaderCircle, LucideAngularModule, Minus, Plus, ShieldCheck } from 'lucide-angular';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { AddOnItem, BookingRequest, DEPOSIT_AMOUNT, Membership, Quote, VenueDetail } from '../../core/api.types';
import { policyLines } from '../../shared/booking-model';
import { dayLabel, localClock, rupees } from '../../shared/format';
import { planTerms } from '../../shared/labels';

/** Booking one slot: extras, a discount code, what it costs and what to pay now, then confirm. */
@Component({
  selector: 'fs-book',
  imports: [FormsModule, RouterLink, LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './book.html',
})
export class Book {
  private readonly api = inject(Api);

  private readonly router = inject(Router);

  readonly slug = input.required<string>();

  readonly court = input.required<string>();

  readonly start = input.required<string>();

  protected readonly icons = { LoaderCircle, Minus, Plus, ShieldCheck };

  protected readonly fmt = { rupees, localClock, dayLabel };

  protected readonly depositAmount = DEPOSIT_AMOUNT;

  protected readonly venue = signal<VenueDetail | null>(null);

  protected readonly extras = signal<AddOnItem[]>([]);

  /** Quantity by add-on id. */
  protected readonly picked = signal<Partial<Record<string, number>>>({});

  protected readonly code = signal('');

  /** The code sent with the quote; set by Apply. */
  protected readonly appliedCode = signal<string | null>(null);

  protected readonly notes = signal('');

  protected readonly terms = planTerms;

  /** The player's active memberships at this venue. */
  protected readonly memberships = signal<Membership[]>([]);

  /** The membership used for this booking, instead of a discount code. */
  protected readonly membershipId = signal<string | null>(null);

  protected readonly quote = signal<Quote | null>(null);

  /** Why this slot can't be booked as asked (taken, too far ahead...). */
  protected readonly problem = signal<string | null>(null);

  protected readonly codeError = signal<string | null>(null);

  protected readonly error = signal<string | null>(null);

  protected readonly busy = signal(false);

  protected readonly courtName = computed(() => this.venue()?.courts.find((c) => c.id === this.court())?.name ?? '');

  protected readonly when = computed(() => {
    const start = this.start();
    return `${dayLabel(start.slice(0, 10))} · ${localClock(start)}`;
  });

  protected readonly rules = computed(() => {
    const venue = this.venue();
    return venue ? policyLines(venue.policy) : [];
  });

  protected readonly depositBlocks = computed(() => (this.quote()?.depositToAdd ?? 0) > 0);

  private readonly request = computed<BookingRequest>(() => ({
    courtId: this.court(),
    startsAt: this.start(),
    membershipId: this.membershipId() ?? undefined,
    discountCode: this.membershipId() ? undefined : this.appliedCode() ?? undefined,
    addOns: Object.entries(this.picked()).flatMap(([itemId, quantity]) => (quantity ? [{ itemId, quantity }] : [])),
  }));

  constructor() {
    effect(() => {
      const slug = this.slug();
      untracked(() => void this.load(slug));
    });
    effect(() => {
      const request = this.request();
      untracked(() => void this.price(request));
    });
  }

  protected change(item: AddOnItem, by: number): void {
    this.picked.update((picked) => ({ ...picked, [item.id]: Math.max(0, Math.min(50, (picked[item.id] ?? 0) + by)) }));
  }

  protected applyCode(): void {
    this.codeError.set(null);
    this.appliedCode.set(this.code().trim().toUpperCase() || null);
  }

  protected removeCode(): void {
    this.code.set('');
    this.appliedCode.set(null);
    this.codeError.set(null);
  }

  protected async confirm(): Promise<void> {
    if (this.busy() || !this.quote() || this.depositBlocks()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      const booking = await this.api.book({ ...this.request(), notes: this.notes().trim() || undefined });
      await this.router.navigate(['/bookings', booking.id], { queryParams: { booked: 1 } });
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  private async load(slug: string): Promise<void> {
    try {
      const [venue, extras, memberships] = await Promise.all([this.api.venue(slug), this.api.addOns(slug), this.api.memberships()]);
      this.venue.set(venue);
      this.extras.set(extras.filter((item) => item.active));
      this.memberships.set(memberships.filter((m) => m.venue.slug === slug && m.status === 'ACTIVE'));
    }
    catch (error) {
      this.problem.set(problemOf(error).message);
    }
  }

  private async price(request: BookingRequest): Promise<void> {
    try {
      this.quote.set(await this.api.quote(request));
      this.problem.set(null);
    }
    catch (error) {
      const problem = problemOf(error);
      if (request.discountCode && /CODE|DISCOUNT/.test(problem.code ?? '')) {
        // A bad code shouldn't hide the price: show why and quote without it
        this.codeError.set(problem.message);
        this.appliedCode.set(null);
        return;
      }
      this.quote.set(null);
      this.problem.set(problem.message);
    }
  }
}
