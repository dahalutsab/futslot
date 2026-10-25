import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LucideAngularModule, Search } from 'lucide-angular';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { VenueSummary } from '../../core/api.types';
import { l } from '../../i18n/i18n';
import { VenueCard } from '../../shared/venue-card';

/** Finding a venue: search by name or area, narrow to a city, and featured venues first. */
@Component({
  selector: 'fs-explore',
  imports: [FormsModule, LucideAngularModule, VenueCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <!-- The turf band, as on FutSlot's front page: the court's half-way line and centre circle in chalk -->
    <div class="relative isolate overflow-hidden bg-turf text-white">
      <svg class="absolute inset-0 -z-10 size-full text-white/15" viewBox="0 0 1200 420" preserveAspectRatio="xMidYMid slice"
        fill="none" stroke="currentColor" stroke-width="3" aria-hidden="true">
        <path d="M920 30v360" />
        <circle cx="920" cy="210" r="96" />
        <circle cx="920" cy="210" r="5" fill="currentColor" />
      </svg>
      <div class="mx-auto max-w-6xl px-4 py-8 lg:px-8 lg:py-12">
        <h1 class="max-w-[14ch] text-5xl font-bold leading-[.9] lg:text-7xl">{{ l().exploreTitle }}</h1>
        <p class="mt-3 max-w-[60ch] text-white/85">{{ l().exploreSubtitle }}</p>

        <form class="mt-6 flex max-w-2xl gap-2" role="search" (ngSubmit)="search()">
          <label for="q" class="sr-only">{{ l().venueSearchHint }}</label>
          <div class="flex h-12 min-w-0 flex-1 items-center gap-2 rounded-md bg-white px-3 text-turf-ink focus-within:outline-3 focus-within:outline-offset-2 focus-within:outline-white">
            <lucide-icon [img]="icons.Search" [size]="18" class="text-turf-hint" />
            <input id="q" name="q" type="search" [placeholder]="l().venueSearchHint" autocomplete="off"
              class="h-full min-w-0 flex-1 bg-transparent text-base outline-none placeholder:text-turf-hint" [ngModel]="query()" (ngModelChange)="query.set($event)" />
          </div>
          <button type="submit" class="h-12 rounded-md bg-turf-ink px-5 font-semibold text-white hover:bg-black">{{ l().search }}</button>
        </form>

        @if (cities().length > 1) {
          <div class="mt-4 flex gap-2 overflow-x-auto pb-1 [scrollbar-width:none]" role="group" [attr.aria-label]="l().city">
            <button type="button" (click)="pickCity(null)" [attr.aria-pressed]="city() === null"
              class="h-10 shrink-0 rounded-full border px-4 text-sm font-medium"
              [class]="city() === null ? 'border-white bg-white text-turf-deep' : 'border-white/45 text-white hover:border-white'">{{ l().allCities }}</button>
            @for (name of cities(); track name) {
              <button type="button" (click)="pickCity(name)" [attr.aria-pressed]="city() === name"
                class="h-10 shrink-0 rounded-full border px-4 text-sm font-medium"
                [class]="city() === name ? 'border-white bg-white text-turf-deep' : 'border-white/45 text-white hover:border-white'">{{ name }}</button>
            }
          </div>
        }
      </div>
    </div>

    <div class="mx-auto max-w-6xl px-4 pb-6 lg:px-8 lg:pb-10">

      @if (featured().length && !filtered()) {
        <section class="mt-8" aria-labelledby="featured-title">
          <h2 id="featured-title" class="text-3xl">{{ l().featured }}</h2>
          <ul class="mt-4 grid gap-x-6 gap-y-10 sm:grid-cols-2 lg:grid-cols-3">
            @for (venue of featured(); track venue.id) {
              <li><fs-venue-card [venue]="venue" /></li>
            }
          </ul>
        </section>
      }

      <section class="mt-8" aria-labelledby="venues-title">
        <h2 id="venues-title" class="text-3xl lg:text-4xl">
          {{ filtered() ? l().venueCount(total()) : l().allVenues }}
        </h2>
        @if (error(); as message) {
          <div class="mt-3 rounded-md border border-border bg-surface px-6 py-10 text-center">
            <p class="text-muted">{{ message }}</p>
            <button type="button" (click)="load(0)" class="mt-4 h-11 rounded-md bg-ink px-5 font-medium text-ink-fg">{{ l().tryAgain }}</button>
          </div>
        } @else if (venues(); as list) {
          @if (list.length === 0) {
            <div class="mt-3 rounded-md border border-dashed border-border bg-surface px-6 py-12 text-center">
              <p class="font-medium">{{ l().noVenuesFound }}</p>
              <p class="mt-1 text-muted">{{ l().noVenuesFoundBody }}</p>
            </div>
          } @else {
            <ul class="mt-4 grid gap-x-6 gap-y-10 sm:grid-cols-2 lg:grid-cols-3">
              @for (venue of list; track venue.id) {
                <li><fs-venue-card [venue]="venue" /></li>
              }
            </ul>
            @if (hasMore()) {
              <div class="mt-6 text-center">
                <button type="button" (click)="load(page() + 1)" [disabled]="loading()"
                  class="h-11 rounded-md border border-border bg-surface px-6 font-medium hover:bg-surface-2 disabled:opacity-60">
                  {{ loading() ? l().loadingMore : l().showMoreVenues }}
                </button>
              </div>
            }
          }
        } @else {
          <ul class="mt-3 grid gap-4 sm:grid-cols-2 lg:grid-cols-3" [attr.aria-label]="l().loading">
            @for (i of [1, 2, 3]; track i) {
              <li class="h-64 animate-pulse rounded-md bg-surface"></li>
            }
          </ul>
        }
      </section>
    </div>
  `,
})
export class Explore {
  protected readonly l = l;

  private readonly api = inject(Api);

  private readonly router = inject(Router);

  private readonly route = inject(ActivatedRoute);

  protected readonly icons = { Search };

  /** What's typed; {@link q} is what was searched. */
  protected readonly query = signal('');

  private readonly q = signal<string | null>(null);

  protected readonly city = signal<string | null>(null);

  protected readonly venues = signal<VenueSummary[] | null>(null);

  protected readonly featured = signal<VenueSummary[]>([]);

  protected readonly page = signal(0);

  protected readonly total = signal(0);

  protected readonly hasMore = signal(false);

  protected readonly loading = signal(false);

  protected readonly error = signal<string | null>(null);

  /** Cities seen in the unfiltered list, so the chips only offer places that have venues. */
  private readonly knownCities = signal<string[]>([]);

  protected readonly cities = computed(() => {
    const selected = this.city();
    const known = this.knownCities();
    return selected && !known.includes(selected) ? [...known, selected] : known;
  });

  protected readonly filtered = computed(() => this.q() !== null || this.city() !== null);

  constructor() {
    const params = this.route.snapshot.queryParamMap;
    this.query.set(params.get('q') ?? '');
    this.q.set(params.get('q'));
    this.city.set(params.get('city'));
    effect(() => {
      this.q();
      this.city();
      untracked(() => void this.load(0));
    });
    void this.api.featured().then((list) => this.featured.set(list), () => undefined);
  }

  protected search(): void {
    const q = this.query().trim();
    this.q.set(q || null);
    this.remember();
  }

  protected pickCity(city: string | null): void {
    this.city.set(city);
    this.remember();
  }

  protected async load(page: number): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    if (page === 0) {
      this.venues.set(null);
    }
    try {
      const result = await this.api.venues({ q: this.q() ?? undefined, city: this.city() ?? undefined, page });
      this.venues.set(page === 0 ? result.content : [...(this.venues() ?? []), ...result.content]);
      this.page.set(page);
      this.total.set(result.totalElements);
      this.hasMore.set(page + 1 < result.totalPages);
      if (!this.filtered()) {
        const seen = new Set([...this.knownCities(), ...result.content.map((venue) => venue.city)]);
        this.knownCities.set([...seen].sort());
      }
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
    finally {
      this.loading.set(false);
    }
  }

  private remember(): void {
    void this.router.navigate([], { queryParams: { q: this.q(), city: this.city() }, replaceUrl: true });
  }
}
