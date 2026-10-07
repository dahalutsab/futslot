import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Check, CircleCheck, LucideAngularModule, Phone, Undo2, Wallet } from 'lucide-angular';

import { problemOf } from '../../core/api-error';
import { Payment, Refund } from '../../core/api.types';
import { VenueContext } from '../../core/venue-context';
import { Badges } from '../../layout/badges';
import { ago, methodLabel, phone, rupees } from '../../shared/format';
import { PaymentsApi } from './payments-api';

type Tab = 'verify' | 'refunds' | 'refunded';

/**
 * The money queue. "To verify": wallet payments players reported, to check against the venue's own wallet.
 * "Refunds to pay": what cancellations left owed, to send back the way it came. Each item carries its
 * own action so a busy desk can clear the list top to bottom.
 */
@Component({
  selector: 'fs-payments',
  imports: [FormsModule, LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './payments.html',
})
export class Payments {
  private readonly api = inject(PaymentsApi);

  private readonly badges = inject(Badges);

  private readonly router = inject(Router);

  protected readonly venues = inject(VenueContext);

  protected readonly icons = { Check, CircleCheck, Phone, Undo2, Wallet };

  protected readonly fmt = { rupees, methodLabel, phone, ago };

  protected readonly tab = signal<Tab>((inject(ActivatedRoute).snapshot.queryParamMap.get('tab') as Tab) ?? 'verify');

  protected readonly pending = signal<Payment[] | null>(null);

  protected readonly refunds = signal<Refund[] | null>(null);

  protected readonly refunded = signal<Refund[] | null>(null);

  protected readonly error = signal<string | null>(null);

  /** The item whose second step (reject reason, refund reference) is open. */
  protected readonly expanded = signal<string | null>(null);

  protected readonly note = signal('');

  protected readonly busy = signal<string | null>(null);

  protected readonly rowError = signal<{ id: string; message: string } | null>(null);

  protected readonly owed = computed(() => (this.refunds() ?? []).reduce((sum, refund) => sum + refund.amount, 0));

  constructor() {
    effect(() => {
      const venue = this.venues.current();
      if (venue) {
        void this.load(venue.venue.id);
      }
    });
  }

  protected show(tab: Tab): void {
    this.tab.set(tab);
    this.expanded.set(null);
    void this.router.navigate([], { queryParams: { tab }, replaceUrl: true });
  }

  protected purpose(payment: Payment): string {
    switch (payment.purpose) {
      case 'MEMBERSHIP':
        return `Membership · ${payment.membershipPlanName}`;
      case 'TOURNAMENT_ENTRY':
        return `Tournament entry · ${payment.tournamentName}`;
      default:
        return `Booking ${payment.bookingReference}`;
    }
  }

  protected open(id: string): void {
    this.expanded.set(this.expanded() === id ? null : id);
    this.note.set('');
    this.rowError.set(null);
  }

  protected approve(payment: Payment): Promise<void> {
    return this.act(payment.id, (venueId) => this.api.approve(venueId, payment.id));
  }

  protected reject(payment: Payment): Promise<void> {
    if (this.note().trim().length < 3) {
      this.rowError.set({ id: payment.id, message: 'Say why, so the player knows what to fix.' });
      return Promise.resolve();
    }
    return this.act(payment.id, (venueId) => this.api.reject(venueId, payment.id, this.note().trim()));
  }

  protected markRefunded(refund: Refund): Promise<void> {
    return this.act(refund.id, (venueId) => this.api.refunded(venueId, refund.id, this.note().trim()));
  }

  protected retry(): void {
    const venue = this.venues.current();
    if (venue) {
      void this.load(venue.venue.id);
    }
  }

  private async act(id: string, action: (venueId: string) => Promise<unknown>): Promise<void> {
    const venue = this.venues.current();
    if (!venue || this.busy()) {
      return;
    }
    this.busy.set(id);
    this.rowError.set(null);
    try {
      await action(venue.venue.id);
      this.expanded.set(null);
      await this.load(venue.venue.id);
      void this.badges.refresh();
    }
    catch (error) {
      this.rowError.set({ id, message: problemOf(error).message });
    }
    finally {
      this.busy.set(null);
    }
  }

  private async load(venueId: string): Promise<void> {
    this.error.set(null);
    try {
      const [pending, refunds, refunded] = await Promise.all([
        this.api.pending(venueId),
        this.api.refunds(venueId, 'REQUESTED'),
        this.api.refunds(venueId, 'SUCCEEDED'),
      ]);
      this.pending.set(pending);
      this.refunds.set(refunds);
      this.refunded.set(refunded.slice().reverse());
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
