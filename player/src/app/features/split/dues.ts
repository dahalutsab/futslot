import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { OwedShare } from '../../core/api.types';
import { dayLabel, localClock, phone, rupees } from '../../shared/format';

/** What the player owes friends who booked games they played in. */
@Component({
  selector: 'fs-dues',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-2xl px-4 py-6 lg:py-10">
      <h1 class="text-2xl font-semibold tracking-tight">What you owe</h1>
      <p class="mt-1 text-muted">Your share of games other players booked. Pay them, and they tick you off.</p>
      @if (error(); as message) {
        <p class="mt-4 text-danger" role="alert">{{ message }}</p>
      } @else if (dues(); as list) {
        @if (list.length === 0) {
          <div class="mt-6 rounded-xl border border-dashed border-border bg-surface px-6 py-12 text-center">
            <p class="font-medium">You're all square</p>
            <p class="mt-1 text-muted">When someone adds you to a game they booked and splits the cost, your share shows up here.</p>
          </div>
        } @else {
          <p class="mt-4"><span class="tabular text-2xl font-semibold">{{ fmt.rupees(total()) }}</span> <span class="text-muted">in all</span></p>
          <ul class="mt-3 space-y-3">
            @for (d of list; track d.bookingId) {
              <li>
                <a [routerLink]="['/bookings', d.bookingId, 'split']" class="flex items-start gap-3 rounded-xl border border-border bg-surface p-4 hover:border-subtle">
                  <div class="min-w-0 flex-1">
                    <p class="font-medium">{{ d.payTo.name }}</p>
                    <p class="tabular text-sm text-muted">{{ d.venueName }} · {{ fmt.dayLabel(d.startsAt.slice(0, 10)) }} · {{ fmt.localClock(d.startsAt) }}</p>
                    @if (d.payTo.phone) {
                      <p class="tabular mt-1 text-sm">Pay to {{ fmt.phone(d.payTo.phone) }}</p>
                    }
                  </div>
                  <p class="tabular font-semibold">{{ fmt.rupees(d.amount) }}</p>
                </a>
              </li>
            }
          </ul>
        }
      } @else {
        <div class="mt-6 h-32 animate-pulse rounded-xl bg-surface" aria-label="Loading"></div>
      }
    </div>
  `,
})
export class Dues {
  private readonly api = inject(Api);

  protected readonly fmt = { rupees, dayLabel, localClock, phone };

  protected readonly dues = signal<OwedShare[] | null>(null);

  protected readonly error = signal<string | null>(null);

  protected readonly total = () => (this.dues() ?? []).reduce((sum, d) => sum + d.amount, 0);

  constructor() {
    this.api.splitDues().then((list) => this.dues.set(list), (error) => this.error.set(problemOf(error).message));
  }
}
