import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { VenueSummary } from '../../core/api.types';
import { l } from '../../i18n/i18n';
import { VenueCard } from '../../shared/venue-card';

/** Finding a venue: search by name or area, narrow to a city, and featured venues first. */
@Component({
  selector: 'fs-explore',
  imports: [FormsModule, VenueCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <!-- The front page's hero, on paper: the heading flush left, search, then the cities as links -->
    <div class="mx-auto max-w-[1280px] px-5 pt-8 pb-10 lg:px-[72px] lg:pt-14 lg:pb-14">
      <h1 class="ml-[-0.035em] max-w-[16ch] text-[40px] leading-[44px] tracking-[-0.02em] lg:text-[72px] lg:leading-[76px] lg:tracking-[-0.025em]">{{ l().exploreTitle }}</h1>
      <p class="mt-4 max-w-[50ch] text-[17px] leading-7 text-text/82 lg:mt-6 lg:text-[19px] lg:leading-[30px]">{{ l().exploreSubtitle }}</p>

      <form class="mt-6 flex max-w-[520px] gap-2.5 lg:mt-8" role="search" (ngSubmit)="search()">
        <label for="q" class="sr-only">{{ l().venueSearchHint }}</label>
        <input id="q" name="q" type="search" [placeholder]="l().venueSearchHint" autocomplete="off"
          class="min-h-11 min-w-0 flex-1 rounded-md border border-divider bg-surface-2 px-2.5 text-base caret-accent placeholder:text-text/65 hover:border-text/45 focus-visible:border-accent focus-visible:outline-offset-0"
          [ngModel]="query()" (ngModelChange)="query.set($event)" />
        <button type="submit" class="min-h-11 rounded-md bg-accent px-[22px] text-[15px] font-semibold text-bg hover:bg-accent-600 active:bg-accent-700">{{ l().search }}</button>
      </form>

      @if (cities().length > 1) {
        <div class="mt-4 flex flex-wrap gap-x-7 text-[15px]" role="group" [attr.aria-label]="l().city">
          <button type="button" (click)="pickCity(null)" [attr.aria-pressed]="city() === null"
            class="min-h-11" [class]="city() === null ? 'text-text underline decoration-1 underline-offset-[5px]' : 'text-accent-700 hover:text-accent-800'">{{ l().allCities }}</button>
          @for (name of cities(); track name) {
            <button type="button" (click)="pickCity(name)" [attr.aria-pressed]="city() === name"
              class="min-h-11" [class]="city() === name ? 'text-text underline decoration-1 underline-offset-[5px]' : 'text-accent-700 hover:text-accent-800'">{{ name }}</button>
          }
        </div>
      }
    </div>

    <div class="mx-auto max-w-[1280px] px-5 pb-6 lg:px-[72px] lg:pb-10">

      @if (featured().length && !filtered()) {
        <section class="mt-6" aria-labelledby="featured-title">
          <h2 id="featured-title" class="kicker">{{ l().featured }}</h2>
          <ul class="mt-7 grid gap-x-12 gap-y-14 sm:grid-cols-2 lg:grid-cols-3">
            @for (venue of featured(); track venue.id) {
              <li><fs-venue-card [venue]="venue" /></li>
            }
          </ul>
        </section>
      }

      <section class="mt-14" aria-labelledby="venues-title">
        <h2 id="venues-title" class="kicker">
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
            <ul class="mt-7 grid gap-x-12 gap-y-14 sm:grid-cols-2 lg:grid-cols-3">
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
