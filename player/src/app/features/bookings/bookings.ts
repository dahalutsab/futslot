import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { Booking } from '../../core/api.types';
import { l } from '../../i18n/i18n';
import { bookingChip } from '../../shared/booking-model';
import { calendarParts, dayLabel, localClock, rupees } from '../../shared/format';
import { StatusChip } from '../../shared/status-chip';

type Scope = 'upcoming' | 'past';

const PAGE = 20;

/** The player's bookings everywhere: what's coming up, and what's been played or cancelled. */
@Component({
  selector: 'fs-bookings',
  imports: [RouterLink, StatusChip],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-3xl px-4 py-6 lg:px-8 lg:py-10">
      <h1 class="text-2xl font-semibold tracking-tight">{{ l().yourBookings }}</h1>

      <div class="mt-4 flex gap-1 rounded-lg bg-surface-2 p-1" role="tablist" [attr.aria-label]="l().tabBookings">
        @for (option of scopes(); track option.value) {
          <button type="button" role="tab" [attr.aria-selected]="scope() === option.value" (click)="show(option.value)"
            class="h-10 flex-1 rounded-md font-medium text-muted"
            [class.bg-surface]="scope() === option.value" [class.text-text]="scope() === option.value">{{ option.label }}</button>
        }
      </div>

      @if (error(); as message) {
        <div class="mt-6 rounded-xl border border-border bg-surface px-6 py-10 text-center">
          <p class="text-muted">{{ message }}</p>
          <button type="button" (click)="load(0)" class="mt-4 h-11 rounded-lg bg-ink px-5 font-medium text-ink-fg">{{ l().tryAgain }}</button>
        </div>
      } @else if (bookings(); as list) {
        @if (list.length === 0) {
          <div class="mt-6 rounded-xl border border-dashed border-border bg-surface px-6 py-12 text-center">
            <p class="font-medium">{{ scope() === 'upcoming' ? l().noGamesComingUp : l().nothingHereYet }}</p>
            <p class="mt-1 text-muted">{{ scope() === 'upcoming' ? l().findCourtPitch : l().pastGamesShowHere }}</p>
            @if (scope() === 'upcoming') {
              <a routerLink="/explore" class="mt-4 inline-flex h-11 items-center rounded-lg bg-ink px-5 font-medium text-ink-fg">{{ l().findACourt }}</a>
            }
          </div>
        } @else {
          <ul class="mt-4 space-y-3">
            @for (booking of list; track booking.id) {
              <li>
                <a [routerLink]="['/bookings', booking.id]" class="flex items-start gap-4 rounded-xl border border-border bg-surface p-4 hover:border-subtle">
                  @let date = dateParts(booking.startsAt);
                  <div class="w-14 shrink-0 text-center" aria-hidden="true">
                    <p class="text-xs font-medium uppercase text-muted">{{ date.weekday }}</p>
                    <p class="tabular text-2xl font-semibold leading-tight">{{ date.day }}</p>
                    <p class="text-xs text-muted">{{ date.month }}</p>
                  </div>
                  <div class="min-w-0 flex-1">
                    <p class="font-medium">{{ booking.venue.name }}</p>
                    <p class="tabular text-sm text-muted"><span class="sr-only">{{ fmt.dayLabel(booking.startsAt.slice(0, 10)) }}, </span>{{ fmt.localClock(booking.startsAt) }} – {{ fmt.localClock(booking.endsAt) }} · {{ booking.court.name }}</p>
                    <div class="mt-2 flex flex-wrap items-center gap-2">
                      <fs-status-chip [value]="chip(booking)" />
                      <span class="tabular text-sm text-muted">{{ fmt.rupees(booking.totalAmount) }}</span>
                    </div>
                  </div>
                </a>
              </li>
            }
          </ul>
          @if (hasMore()) {
            <div class="mt-6 text-center">
              <button type="button" (click)="load(page() + 1)" [disabled]="loading()"
                class="h-11 rounded-lg border border-border bg-surface px-6 font-medium hover:bg-surface-2 disabled:opacity-60">
                {{ loading() ? l().loadingMore : l().showMore }}
              </button>
            </div>
          }
        }
      } @else {
        <div class="mt-6 space-y-3" [attr.aria-label]="l().loading">
          @for (i of [1, 2, 3]; track i) {
            <div class="h-24 animate-pulse rounded-xl bg-surface"></div>
          }
        </div>
      }
    </div>
  `,
})
export class Bookings {
  protected readonly l = l;

  private readonly api = inject(Api);

  private readonly router = inject(Router);

  protected readonly fmt = { rupees, localClock, dayLabel };

  protected readonly scopes = computed<{ value: Scope; label: string }[]>(() => [
    { value: 'upcoming', label: l().upcoming },
    { value: 'past', label: l().past },
  ]);

  protected readonly scope = signal<Scope>(inject(ActivatedRoute).snapshot.queryParamMap.get('scope') === 'past' ? 'past' : 'upcoming');

  protected readonly bookings = signal<Booking[] | null>(null);

  protected readonly page = signal(0);

  protected readonly hasMore = signal(false);

  protected readonly loading = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly chip = (booking: Booking) => bookingChip(booking);

  /** "Thu", "9", "Oct" for the venue-local day a booking starts. */
  protected dateParts(startsAt: string): { weekday: string; day: string; month: string } {
    return calendarParts(startsAt.slice(0, 10));
  }

  constructor() {
    effect(() => {
      this.scope();
      untracked(() => void this.load(0));
    });
  }

  protected show(scope: Scope): void {
    this.scope.set(scope);
    void this.router.navigate([], { queryParams: { scope: scope === 'past' ? 'past' : null }, replaceUrl: true });
  }

  protected async load(page: number): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    if (page === 0) {
      this.bookings.set(null);
    }
    try {
      const list = await this.api.bookings(this.scope(), page);
      this.bookings.set(page === 0 ? list : [...(this.bookings() ?? []), ...list]);
      this.page.set(page);
      this.hasMore.set(list.length === PAGE);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
    finally {
      this.loading.set(false);
    }
  }
}
