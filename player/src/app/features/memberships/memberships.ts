import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { Membership } from '../../core/api.types';
import { dayLabel } from '../../shared/format';
import { planTerms } from '../../shared/labels';

/** The player's game packs and passes, at every venue. */
@Component({
  selector: 'fs-memberships',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-2xl px-4 py-6 lg:py-10">
      <h1 class="text-2xl font-semibold tracking-tight">Memberships</h1>
      <p class="mt-1 text-muted">Game packs and passes you bought at venues. They're used when you book there.</p>
      @if (error(); as message) {
        <p class="mt-4 text-danger" role="alert">{{ message }}</p>
      } @else if (memberships(); as list) {
        @if (list.length === 0) {
          <div class="mt-6 rounded-xl border border-dashed border-border bg-surface px-6 py-12 text-center">
            <p class="font-medium">No memberships yet</p>
            <p class="mt-1 text-muted">Venues that sell game packs or passes list them on their page.</p>
          </div>
        } @else {
          <ul class="mt-4 space-y-3">
            @for (m of list; track m.id) {
              <li class="rounded-xl border p-4" [class]="m.status === 'ACTIVE' ? 'border-border bg-surface' : 'border-border bg-surface-2'">
                <div class="flex items-start gap-3">
                  <div class="min-w-0 flex-1">
                    <p class="font-semibold">{{ m.plan.name }}</p>
                    <a [routerLink]="['/venues', m.venue.slug]" class="text-sm text-muted hover:underline">{{ m.venue.name }}</a>
                  </div>
                  <span class="rounded-full px-2 py-0.5 text-xs font-medium"
                    [class]="m.status === 'ACTIVE' ? 'bg-confirmed-bg text-confirmed-fg' : m.status === 'PENDING_PAYMENT' ? 'bg-verify-bg text-verify-fg' : 'bg-surface text-muted'">
                    {{ m.status === 'ACTIVE' ? 'Active' : m.status === 'PENDING_PAYMENT' ? 'Payment being checked' : m.status === 'EXPIRED' ? 'Expired' : 'Cancelled' }}
                  </span>
                </div>
                <p class="mt-2 text-sm">
                  {{ terms(m.plan) }}
                  @if (m.gamesRemaining !== null) { · <span class="tabular font-medium">{{ m.gamesRemaining }} left</span> }
                </p>
                <p class="text-sm text-muted">{{ m.status === 'EXPIRED' ? 'Ended' : 'Until' }} {{ fmt.dayLabel(m.expiresOn) }}</p>
                @if (m.status === 'ACTIVE') {
                  <a [routerLink]="['/venues', m.venue.slug]" class="mt-3 inline-flex h-11 items-center rounded-lg bg-ink px-4 font-medium text-ink-fg">Book with it</a>
                }
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
export class Memberships {
  private readonly api = inject(Api);

  protected readonly fmt = { dayLabel };

  protected readonly terms = planTerms;

  protected readonly memberships = signal<Membership[] | null>(null);

  protected readonly error = signal<string | null>(null);

  constructor() {
    this.api.memberships().then((list) => this.memberships.set(list), (error) => this.error.set(problemOf(error).message));
  }
}
