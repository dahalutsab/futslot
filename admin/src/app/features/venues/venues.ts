import { ChangeDetectionStrategy, Component, effect, inject, signal, untracked } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { VenueStatus, VenueSummary } from '../../core/api.types';
import { VENUE_STATUS } from '../../shared/labels';

const TABS: VenueStatus[] = ['PENDING_APPROVAL', 'ACTIVE', 'SUSPENDED', 'REJECTED', 'DRAFT'];

/** Venues by status; new ones waiting for approval come first. */
@Component({
  selector: 'fs-venues',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-5xl px-4 py-6 lg:px-8 lg:py-8">
      <h1 class="text-3xl lg:text-4xl">Venues</h1>
      <div class="mt-4 flex gap-1 overflow-x-auto rounded-md bg-surface-2 p-1 [scrollbar-width:none]" role="tablist" aria-label="Venue status">
        @for (s of tabs; track s) {
          <button type="button" role="tab" [attr.aria-selected]="status() === s" (click)="show(s)"
            class="h-10 shrink-0 rounded-md px-4 font-medium text-muted" [class.bg-surface]="status() === s" [class.text-text]="status() === s">{{ label[s] }}</button>
        }
      </div>
      @if (error(); as message) {
        <p class="mt-4 text-danger" role="alert">{{ message }}</p>
      } @else if (venues(); as list) {
        @if (list.length === 0) {
          <p class="mt-6 rounded-md border border-dashed border-border bg-surface px-6 py-12 text-center text-muted">
            {{ status() === 'PENDING_APPROVAL' ? 'No venues waiting. New ones show up here when owners submit them.' : 'None.' }}
          </p>
        } @else {
          <div class="mt-4 overflow-x-auto rounded-md border border-border bg-surface">
            <table class="w-full min-w-[520px] text-left">
              <thead class="border-b border-border text-sm text-muted">
                <tr><th scope="col" class="px-4 py-2.5 font-medium">Venue</th><th scope="col" class="px-4 py-2.5 font-medium">Where</th><th scope="col" class="px-4 py-2.5 text-right font-medium">Courts</th><th scope="col" class="px-4 py-2.5 text-right font-medium">Rating</th></tr>
              </thead>
              <tbody>
                @for (v of list; track v.id) {
                  <tr class="border-b border-border last:border-0 hover:bg-surface-2">
                    <td class="px-4 py-3"><a [routerLink]="['/venues', v.id]" class="font-medium hover:underline">{{ v.name }}</a></td>
                    <td class="px-4 py-3 text-muted">{{ v.area ? v.area + ', ' : '' }}{{ v.city }}</td>
                    <td class="tabular px-4 py-3 text-right">{{ v.courtCount }}</td>
                    <td class="tabular px-4 py-3 text-right">{{ v.reviewCount ? v.averageRating.toFixed(1) + ' (' + v.reviewCount + ')' : '–' }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          @if (total() > list.length) {
            <p class="mt-3 text-sm text-muted">Showing {{ list.length }} of {{ total() }}.</p>
          }
        }
      } @else {
        <div class="mt-6 h-40 animate-pulse rounded-md bg-surface" aria-label="Loading"></div>
      }
    </div>
  `,
})
export class Venues {
  private readonly api = inject(Api);

  private readonly router = inject(Router);

  protected readonly tabs = TABS;

  protected readonly label = VENUE_STATUS;

  protected readonly status = signal<VenueStatus>(this.initial(inject(ActivatedRoute).snapshot.queryParamMap.get('status')));

  protected readonly venues = signal<VenueSummary[] | null>(null);

  protected readonly total = signal(0);

  protected readonly error = signal<string | null>(null);

  constructor() {
    effect(() => {
      const status = this.status();
      untracked(() => void this.load(status));
    });
  }

  protected show(status: VenueStatus): void {
    this.status.set(status);
    void this.router.navigate([], { queryParams: { status }, replaceUrl: true });
  }

  private initial(value: string | null): VenueStatus {
    return TABS.includes(value as VenueStatus) ? value as VenueStatus : 'PENDING_APPROVAL';
  }

  private async load(status: VenueStatus): Promise<void> {
    this.venues.set(null);
    this.error.set(null);
    try {
      const page = await this.api.venues(status);
      this.venues.set(page.content);
      this.total.set(page.totalElements);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
