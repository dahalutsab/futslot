import { ChangeDetectionStrategy, Component, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { PlatformReport, VenueStatus } from '../../core/api.types';
import { Queue } from '../../core/queue';
import { addDays, dayLabel, rupees, today } from '../../shared/format';
import { VENUE_STATUS } from '../../shared/labels';

const NEPAL = 'Asia/Kathmandu';

type Period = 'month' | '30' | 'year';

/** What's waiting on FutSlot staff, and how the platform is doing over a period. */
@Component({
  selector: 'fs-overview',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-5xl px-4 py-6 lg:px-8 lg:py-8">
      <h1 class="text-3xl lg:text-4xl">Overview</h1>

      <section class="mt-5" aria-labelledby="waiting-title">
        <h2 id="waiting-title" class="font-semibold">Waiting on you</h2>
        <ul class="mt-2 grid grid-cols-2 gap-3 lg:grid-cols-4">
          @for (item of waiting; track item.path) {
            <li>
              <a [routerLink]="item.path" class="block rounded-md border bg-surface p-4 hover:border-subtle"
                [class]="queue.counts()[item.key] ? 'border-verify-line' : 'border-border'">
                <p class="tabular font-display text-4xl font-semibold leading-none" [class.text-subtle]="!queue.counts()[item.key]">{{ queue.counts()[item.key] }}</p>
                <p class="text-sm text-muted">{{ item.label }}</p>
              </a>
            </li>
          }
        </ul>
      </section>

      <section class="mt-8" aria-labelledby="numbers-title">
        <div class="flex flex-wrap items-center gap-3">
          <h2 id="numbers-title" class="flex-1 font-semibold">How FutSlot is doing</h2>
          <div class="flex rounded-md bg-surface-2 p-1" role="radiogroup" aria-label="Period">
            @for (p of periods; track p.value) {
              <button type="button" role="radio" [attr.aria-checked]="period() === p.value" (click)="period.set(p.value)"
                class="h-9 rounded-md px-3 text-sm font-medium text-muted" [class.bg-surface]="period() === p.value" [class.text-text]="period() === p.value">{{ p.label }}</button>
            }
          </div>
        </div>
        @if (error(); as message) {
          <p class="mt-3 text-danger" role="alert">{{ message }}</p>
        } @else if (report(); as r) {
          <p class="mt-1 text-sm text-muted">{{ fmt.dayLabel(r.from) }} – {{ fmt.dayLabel(r.to) }}</p>
          <dl class="mt-3 grid grid-cols-2 gap-3 lg:grid-cols-3">
            @for (k of kpis(r); track k.label) {
              <div class="rounded-md border border-border bg-surface p-4">
                <dt class="text-sm text-muted">{{ k.label }}</dt>
                <dd class="tabular mt-1 font-display text-4xl font-semibold leading-none">{{ k.value }}</dd>
              </div>
            }
          </dl>

          <div class="mt-6 grid gap-6 lg:grid-cols-[1fr_260px]">
            <section aria-labelledby="top-title">
              <h3 id="top-title" class="font-semibold">Busiest venues</h3>
              @if (r.topVenues.length === 0) {
                <p class="mt-2 text-muted">No bookings in this period.</p>
              } @else {
                <div class="mt-2 overflow-x-auto rounded-md border border-border bg-surface">
                  <table class="w-full min-w-[420px] text-left text-sm">
                    <thead class="border-b border-border text-muted">
                      <tr><th scope="col" class="px-4 py-2.5 font-medium">Venue</th><th scope="col" class="px-4 py-2.5 text-right font-medium">Bookings</th><th scope="col" class="px-4 py-2.5 text-right font-medium">Booked value</th></tr>
                    </thead>
                    <tbody>
                      @for (v of r.topVenues; track v.venueId) {
                        <tr class="border-b border-border last:border-0">
                          <td class="px-4 py-2.5"><a [routerLink]="['/venues', v.venueId]" class="font-medium hover:underline">{{ v.name }}</a><span class="text-muted"> · {{ v.city }}</span></td>
                          <td class="tabular px-4 py-2.5 text-right">{{ v.bookings }}</td>
                          <td class="tabular px-4 py-2.5 text-right">{{ fmt.rupees(v.bookedValue) }}</td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
              }
            </section>
            <section aria-labelledby="venues-title">
              <h3 id="venues-title" class="font-semibold">Venues now</h3>
              <ul class="mt-2 divide-y divide-border rounded-md border border-border bg-surface">
                @for (s of statuses; track s) {
                  <li>
                    <a [routerLink]="'/venues'" [queryParams]="{ status: s }" class="flex items-center justify-between px-4 py-2.5 hover:bg-surface-2">
                      <span class="text-muted">{{ venueStatus[s] }}</span><span class="tabular font-medium">{{ r.venuesByStatus[s] ?? 0 }}</span>
                    </a>
                  </li>
                }
              </ul>
            </section>
          </div>
        } @else {
          <div class="mt-3 h-48 animate-pulse rounded-md bg-surface" aria-label="Loading"></div>
        }
      </section>
    </div>
  `,
})
export class Overview {
  private readonly api = inject(Api);

  protected readonly queue = inject(Queue);

  protected readonly fmt = { rupees, dayLabel };

  protected readonly venueStatus = VENUE_STATUS;

  protected readonly statuses: VenueStatus[] = ['ACTIVE', 'PENDING_APPROVAL', 'SUSPENDED', 'REJECTED', 'DRAFT'];

  protected readonly waiting = [
    { path: '/venues', key: 'venues' as const, label: 'Venues to approve' },
    { path: '/payments', key: 'payments' as const, label: 'Payments to check' },
    { path: '/withdrawals', key: 'withdrawals' as const, label: 'Deposits to send' },
    { path: '/payouts', key: 'payouts' as const, label: 'Payouts to venues' },
  ];

  protected readonly periods: { value: Period; label: string }[] = [
    { value: 'month', label: 'This month' },
    { value: '30', label: 'Last 30 days' },
    { value: 'year', label: 'This year' },
  ];

  protected readonly period = signal<Period>('30');

  protected readonly report = signal<PlatformReport | null>(null);

  protected readonly error = signal<string | null>(null);

  constructor() {
    void this.queue.refresh();
    effect(() => {
      const period = this.period();
      untracked(() => void this.load(period));
    });
  }

  protected kpis(r: PlatformReport): { label: string; value: string }[] {
    return [
      { label: 'Bookings', value: String(r.bookings) },
      { label: 'Played', value: String(r.playedBookings) },
      { label: 'Booked value', value: rupees(r.bookedValue) },
      { label: 'Venues with bookings', value: String(r.venuesWithBookings) },
      { label: 'New venues', value: String(r.newVenues) },
      { label: 'New users', value: String(r.newUsers) },
    ];
  }

  private async load(period: Period): Promise<void> {
    const to = today(NEPAL);
    const from = period === 'month' ? `${to.slice(0, 8)}01` : period === 'year' ? `${to.slice(0, 5)}01-01` : addDays(to, -29);
    this.report.set(null);
    this.error.set(null);
    try {
      this.report.set(await this.api.report(from, to));
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
