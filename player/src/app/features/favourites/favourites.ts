import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { VenueSummary } from '../../core/api.types';
import { l } from '../../i18n/i18n';
import { VenueCard } from '../../shared/venue-card';

/** Venues the player saved, to book again quickly. */
@Component({
  selector: 'fs-favourites',
  imports: [RouterLink, VenueCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-[1280px] px-5 py-6 lg:px-[72px] lg:py-10">
      <h1 class="text-4xl">{{ l().meFavourites }}</h1>
      @if (error(); as message) {
        <p class="mt-6 text-muted">{{ message }}</p>
      } @else if (venues(); as list) {
        @if (list.length === 0) {
          <div class="mt-6 rounded-md border border-dashed border-border bg-surface px-6 py-12 text-center">
            <p class="font-medium">{{ l().noSavedVenues }}</p>
            <p class="mt-1 text-muted">{{ l().noSavedVenuesWebBody }}</p>
            <a routerLink="/explore" class="mt-4 inline-flex h-11 items-center rounded-md bg-ink px-5 font-medium text-ink-fg">{{ l().findACourt }}</a>
          </div>
        } @else {
          <ul class="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            @for (venue of list; track venue.id) {
              <li><fs-venue-card [venue]="venue" /></li>
            }
          </ul>
        }
      } @else {
        <div class="mt-6 h-64 animate-pulse rounded-md bg-surface" [attr.aria-label]="l().loading"></div>
      }
    </div>
  `,
})
export class Favourites {
  protected readonly l = l;

  private readonly api = inject(Api);

  protected readonly venues = signal<VenueSummary[] | null>(null);

  protected readonly error = signal<string | null>(null);

  constructor() {
    this.api.favourites().then((list) => this.venues.set(list), (error) => this.error.set(problemOf(error).message));
  }
}
