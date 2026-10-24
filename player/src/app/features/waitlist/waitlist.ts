import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { WaitlistEntry } from '../../core/api.types';
import { l } from '../../i18n/i18n';
import { dayLabel, localClock } from '../../shared/format';

/** Booked slots the player is waiting for. When one frees up they're told, and whoever books first gets it. */
@Component({
  selector: 'fs-waitlist',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-2xl px-4 py-6 lg:py-10">
      <h1 class="text-4xl">{{ l().meWaitlist }}</h1>
      <p class="mt-1 text-muted">{{ l().waitlistIntro }}</p>
      @if (error(); as message) {
        <p class="mt-4 text-danger" role="alert">{{ message }}</p>
      }
      @if (entries(); as list) {
        @if (list.length === 0) {
          <div class="mt-6 rounded-md border border-dashed border-border bg-surface px-6 py-12 text-center">
            <p class="font-medium">{{ l().notWaiting }}</p>
            <p class="mt-1 text-muted">{{ l().notWaitingBody }}</p>
          </div>
        } @else {
          <ul class="mt-4 space-y-3">
            @for (entry of list; track entry.id) {
              <li class="flex items-start gap-3 rounded-md border border-border bg-surface p-4">
                <div class="min-w-0 flex-1">
                  <a [routerLink]="['/venues', entry.venueSlug]" [queryParams]="{ date: entry.startsAt.slice(0, 10) }" class="font-medium hover:underline">{{ entry.venueName }}</a>
                  <p class="tabular text-sm text-muted">{{ fmt.dayLabel(entry.startsAt.slice(0, 10)) }} · {{ fmt.localClock(entry.startsAt) }} – {{ fmt.localClock(entry.endsAt) }} · {{ entry.courtName }}</p>
                  @if (entry.status === 'NOTIFIED') {
                    <p class="mt-2 text-sm font-medium text-confirmed-fg">{{ l().itFreedUp }}</p>
                  }
                </div>
                <button type="button" (click)="leave(entry)" class="h-11 shrink-0 rounded-md px-3 text-sm font-medium text-muted hover:bg-surface-2 hover:text-text">{{ l().stopWaiting }}</button>
              </li>
            }
          </ul>
        }
      } @else {
        <div class="mt-6 h-32 animate-pulse rounded-md bg-surface" [attr.aria-label]="l().loading"></div>
      }
    </div>
  `,
})
export class Waitlist {
  protected readonly l = l;

  private readonly api = inject(Api);

  protected readonly fmt = { dayLabel, localClock };

  protected readonly entries = signal<WaitlistEntry[] | null>(null);

  protected readonly error = signal<string | null>(null);

  constructor() {
    void this.load();
  }

  protected async leave(entry: WaitlistEntry): Promise<void> {
    try {
      await this.api.leaveWaitlist(entry.id);
      this.entries.update((list) => list?.filter((e) => e.id !== entry.id) ?? null);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }

  private async load(): Promise<void> {
    try {
      const list = await this.api.waitlist();
      this.entries.set(list.filter((e) => e.status === 'WAITING' || e.status === 'NOTIFIED'));
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
