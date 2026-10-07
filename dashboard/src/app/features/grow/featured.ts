import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { LoaderCircle, LucideAngularModule } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import { problemOf } from '../../core/api-error';
import { FeaturedListing, FeaturedPlacement, FeaturedQuote } from '../../core/api.types';
import { VenueContext } from '../../core/venue-context';
import { addDays, methodLabel, rupees, today } from '../../shared/format';

const ZONE = 'Asia/Kathmandu';

const PLACEMENTS: { value: FeaturedPlacement; label: string; detail: string }[] = [
  { value: 'HOME_CAROUSEL', label: 'Home screen', detail: 'The big carousel every player sees when they open the app.' },
  { value: 'SEARCH_TOP', label: 'Top of search', detail: 'Above the results whenever anyone searches, anywhere in Nepal.' },
  { value: 'CITY_TOP', label: 'Top of search in your city', detail: 'Above the results when players search in your city.' },
];

const LENGTHS = [3, 7, 14, 30];

const WALLETS = ['ESEWA', 'KHALTI', 'FONEPAY', 'BANK_TRANSFER'];

/** "9 Oct" for an instant, in Nepal. */
function nepalDate(instant: string | Date): string {
  return new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', timeZone: ZONE }).format(new Date(instant));
}

/** "9 Oct – 15 Oct"; a listing ends at the start of the day after its last day. */
export function listingDays(listing: Pick<FeaturedListing, 'startsAt' | 'endsAt'>): string {
  return `${nepalDate(listing.startsAt)} – ${nepalDate(new Date(new Date(listing.endsAt).getTime() - 1000))}`;
}

/**
 * Paying FutSlot to show the venue first. Owners pick where and for how long, see the price and whether a
 * place is free, then pay FutSlot's account and send the transaction ID. Owner only.
 */
@Component({
  selector: 'fs-featured',
  imports: [LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-3xl px-4 py-4 lg:px-8 lg:py-6">
      <h1 class="text-xl font-semibold tracking-tight lg:text-2xl">Featured listing</h1>
      <p class="mt-1 text-muted">Show {{ venueName() }} first in the app for a few days, like before a festival or when evenings are quiet.</p>

      @if (!live()) {
        <p class="mt-5 rounded-xl border border-border bg-surface px-4 py-3 text-muted">Your venue can be featured once FutSlot has approved it and it's listed.</p>
      } @else {
        <section class="mt-5 rounded-xl border border-border bg-surface p-4 lg:p-6" aria-labelledby="new-title">
          <h2 id="new-title" class="text-lg font-semibold">Get featured</h2>
          <div class="mt-4 grid gap-4 sm:grid-cols-2">
            <div>
              <label for="feature-start" class="mb-1.5 block font-medium">Starting</label>
              <input id="feature-start" type="date" [min]="todayKey" [value]="start()" (input)="start.set($any($event.target).value)"
                class="h-11 w-full rounded-lg border border-border bg-surface px-3 text-base" />
            </div>
            <fieldset>
              <legend class="mb-1.5 font-medium">For</legend>
              <div class="flex gap-2">
                @for (length of lengths; track length) {
                  <label class="flex h-11 flex-1 cursor-pointer items-center justify-center rounded-lg border text-sm font-medium" [class]="days() === length ? 'border-ink bg-surface-2' : 'border-border text-muted'">
                    <input type="radio" name="days" class="sr-only" [checked]="days() === length" (change)="days.set(length)" />{{ length }} days
                  </label>
                }
              </div>
            </fieldset>
          </div>
          <fieldset class="mt-4">
            <legend class="mb-1.5 font-medium">Where</legend>
            <div class="space-y-2">
              @for (placement of placements; track placement.value) {
                @let quote = quotes()[placement.value];
                <label class="flex cursor-pointer items-start gap-3 rounded-lg border p-3" [class]="chosen() === placement.value ? 'border-ink bg-surface-2' : 'border-border'"
                  [class.opacity-60]="quote && !quote.available">
                  <input type="radio" name="placement" class="mt-1 size-4 accent-current" [checked]="chosen() === placement.value" (change)="chosen.set(placement.value)" />
                  <span class="min-w-0 flex-1">
                    <span class="block font-medium">{{ placement.label }}</span>
                    <span class="text-sm text-muted">{{ placement.detail }}</span>
                  </span>
                  <span class="shrink-0 text-right">
                    @if (quote) {
                      <span class="tabular block font-semibold">{{ fmt.rupees(quote.price) }}</span>
                      <span class="tabular block text-xs text-muted">{{ fmt.rupees(quote.pricePerDay) }} a day</span>
                      @if (alreadyMine(placement.value)) {
                        <span class="block text-xs font-medium text-muted">You're booked then</span>
                      } @else if (!quote.available) {
                        <span class="block text-xs font-medium text-danger">Full those days</span>
                      }
                    } @else {
                      <span class="block h-5 w-16 animate-pulse rounded bg-surface-2"></span>
                    }
                  </span>
                </label>
              }
            </div>
          </fieldset>
          @if (bookError(); as message) {
            <p class="mt-3 text-sm text-danger" role="alert">{{ message }}</p>
          }
          <button type="button" (click)="book()" [disabled]="busy() || !chosenQuote()?.available"
            class="mt-4 flex h-12 w-full items-center justify-center gap-2 rounded-lg bg-ink font-medium text-ink-fg disabled:opacity-50 sm:w-auto sm:px-6">
            @if (busy()) {
              <lucide-icon [img]="icons.LoaderCircle" [size]="18" class="animate-spin" />
            }
            @if (chosenQuote(); as q) {
              {{ q.available ? 'Book for ' + fmt.rupees(q.price) : alreadyMine(chosen()) ? 'Already booked for those days' : 'Pick other days or another place' }}
            } @else {
              Book
            }
          </button>
          <p class="mt-2 text-sm text-subtle">You pay after booking. The place is held while FutSlot checks your payment.</p>
        </section>
      }

      @if (error(); as message) {
        <p class="mt-5 rounded-xl border border-border bg-surface px-6 py-8 text-center text-muted">{{ message }}</p>
      } @else if (listings(); as list) {
        @if (list.length > 0) {
          <section class="mt-8" aria-labelledby="listings-title">
            <h2 id="listings-title" class="text-lg font-semibold">Your listings</h2>
            <ul class="mt-3 space-y-3">
              @for (listing of list; track listing.id) {
                <li class="rounded-xl border border-border bg-surface p-4">
                  <div class="flex flex-wrap items-baseline justify-between gap-2">
                    <span class="font-medium">{{ placementLabel(listing.placement) }}{{ listing.city ? ', ' + listing.city : '' }}</span>
                    <span class="tabular font-medium">{{ fmt.rupees(listing.price) }}</span>
                  </div>
                  <p class="mt-0.5 text-sm text-muted"><span class="tabular">{{ fmt.listingDays(listing) }}</span> · {{ statusText(listing) }}</p>
                  @if (listing.status === 'PENDING_PAYMENT') {
                    @if (listing.paymentPending || paidIds().includes(listing.id)) {
                      <p class="mt-3 rounded-lg bg-surface-2 px-3 py-2 text-sm" role="status">Payment sent. FutSlot will confirm it soon.</p>
                    } @else {
                      <div class="mt-3 rounded-lg bg-surface-2 px-3 py-2">
                        <p class="text-sm font-medium text-muted">Pay FutSlot</p>
                        <p class="mt-0.5 whitespace-pre-line">{{ listing.paymentInstructions || 'Contact FutSlot for payment details.' }}</p>
                      </div>
                      <form class="mt-3 grid gap-2 sm:grid-cols-[auto_1fr_auto] sm:items-end" (submit)="$event.preventDefault(); pay(listing)">
                        <div>
                          <label [for]="'method-' + listing.id" class="mb-1 block text-sm font-medium">Paid with</label>
                          <select [id]="'method-' + listing.id" (change)="method.set($any($event.target).value)" class="h-11 w-full rounded-lg border border-border bg-surface px-3 text-base">
                            @for (wallet of wallets; track wallet) {
                              <option [value]="wallet" [selected]="method() === wallet">{{ fmt.methodLabel(wallet) }}</option>
                            }
                          </select>
                        </div>
                        <div>
                          <label [for]="'txn-' + listing.id" class="mb-1 block text-sm font-medium">Transaction ID</label>
                          <input [id]="'txn-' + listing.id" type="text" maxlength="100" autocomplete="off" [value]="transactionId()" (input)="transactionId.set($any($event.target).value)"
                            class="tabular h-11 w-full rounded-lg border border-border bg-surface px-3 text-base" />
                        </div>
                        <button type="submit" [disabled]="busy()" class="h-11 rounded-lg bg-ink px-4 font-medium text-ink-fg disabled:opacity-60">I've paid</button>
                      </form>
                      <div class="mt-2 flex items-center gap-2">
                        @if (confirmCancel() === listing.id) {
                          <button type="button" (click)="cancel(listing)" [disabled]="busy()" class="h-9 rounded-lg px-2 text-sm font-medium text-danger hover:bg-danger-bg">Cancel this booking?</button>
                          <button type="button" (click)="confirmCancel.set(null)" class="h-9 rounded-lg px-2 text-sm font-medium hover:bg-surface-2">Keep</button>
                        } @else {
                          <button type="button" (click)="confirmCancel.set(listing.id)" class="h-9 rounded-lg px-1 text-sm font-medium text-muted hover:underline">Cancel</button>
                        }
                      </div>
                    }
                    @if (payError() && payingId() === listing.id) {
                      <p class="mt-2 text-sm text-danger" role="alert">{{ payError() }}</p>
                    }
                  }
                </li>
              }
            </ul>
          </section>
        }
      }
    </div>
  `,
})
export class Featured {
  private readonly http = inject(HttpClient);

  private readonly venues = inject(VenueContext);

  protected readonly icons = { LoaderCircle };

  protected readonly fmt = { listingDays, methodLabel, rupees };

  protected readonly placements = PLACEMENTS;

  protected readonly lengths = LENGTHS;

  protected readonly wallets = WALLETS;

  protected readonly todayKey = today(ZONE);

  protected readonly start = signal(addDays(today(ZONE), 1));

  protected readonly days = signal(7);

  protected readonly chosen = signal<FeaturedPlacement>('CITY_TOP');

  protected readonly quotes = signal<Partial<Record<FeaturedPlacement, FeaturedQuote>>>({});

  protected readonly chosenQuote = computed(() => this.quotes()[this.chosen()] ?? null);

  protected readonly listings = signal<FeaturedListing[] | null>(null);

  protected readonly error = signal<string | null>(null);

  protected readonly busy = signal(false);

  protected readonly bookError = signal<string | null>(null);

  protected readonly method = signal('ESEWA');

  protected readonly transactionId = signal('');

  protected readonly payingId = signal<string | null>(null);

  protected readonly payError = signal<string | null>(null);

  protected readonly paidIds = signal<string[]>([]);

  protected readonly confirmCancel = signal<string | null>(null);

  protected readonly venueName = computed(() => this.venues.current()?.venue.name ?? 'your venue');

  protected readonly live = computed(() => this.venues.current()?.venue.status === 'ACTIVE');

  private readonly venueId = computed(() => this.venues.current()?.venue.id ?? null);

  private quoteSequence = 0;

  constructor() {
    effect(() => {
      const id = this.venueId();
      if (id) {
        untracked(() => void this.loadListings(id));
      }
    });
    effect(() => {
      const id = this.venueId();
      const start = this.start();
      const days = this.days();
      if (id && this.live() && start >= this.todayKey) {
        untracked(() => void this.loadQuotes(id, start, days));
      }
    });
  }

  /** Whether one of this venue's own listings already covers some of the chosen days there. */
  protected alreadyMine(placement: FeaturedPlacement): boolean {
    const from = new Date(`${this.start()}T00:00:00+05:45`).getTime();
    const to = new Date(`${addDays(this.start(), this.days())}T00:00:00+05:45`).getTime();
    return (this.listings() ?? []).some((l) => l.placement === placement && !['CANCELLED', 'ENDED'].includes(l.status)
      && new Date(l.startsAt).getTime() < to && new Date(l.endsAt).getTime() > from);
  }

  protected placementLabel(placement: FeaturedPlacement): string {
    return PLACEMENTS.find((p) => p.value === placement)?.label ?? placement;
  }

  protected statusText(listing: FeaturedListing): string {
    switch (listing.status) {
      case 'PENDING_PAYMENT':
        return 'Waiting for payment';
      case 'SCHEDULED':
        return `Starts ${nepalDate(listing.startsAt)}`;
      case 'ACTIVE':
        return 'Live now';
      case 'ENDED':
        return 'Ended';
      case 'CANCELLED':
        return 'Cancelled';
    }
  }

  protected async book(): Promise<void> {
    const id = this.venueId();
    if (!id || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.bookError.set(null);
    try {
      const listing = await firstValueFrom(this.http.post<FeaturedListing>(`/api/v1/owner/venues/${id}/featured`,
        { placement: this.chosen(), startsOn: this.start(), days: this.days() }));
      this.listings.update((list) => [listing, ...(list ?? [])]);
      void this.loadQuotes(id, this.start(), this.days());
    }
    catch (error) {
      this.bookError.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  protected async pay(listing: FeaturedListing): Promise<void> {
    if (this.busy()) {
      return;
    }
    this.payingId.set(listing.id);
    if (this.transactionId().trim().length < 4) {
      this.payError.set('Enter the transaction ID from your payment receipt.');
      return;
    }
    this.busy.set(true);
    this.payError.set(null);
    try {
      await firstValueFrom(this.http.post(`/api/v1/featured-listings/${listing.id}/payments`,
        { method: this.method(), transactionId: this.transactionId().trim(), amount: listing.price }));
      this.paidIds.update((ids) => [...ids, listing.id]);
      this.transactionId.set('');
    }
    catch (error) {
      const problem = problemOf(error);
      if (problem.code === 'PAYMENT_PENDING') {
        this.paidIds.update((ids) => [...ids, listing.id]);
      }
      else {
        this.payError.set(problem.message);
      }
    }
    finally {
      this.busy.set(false);
    }
  }

  protected async cancel(listing: FeaturedListing): Promise<void> {
    const id = this.venueId();
    if (!id) {
      return;
    }
    this.busy.set(true);
    try {
      const updated = await firstValueFrom(this.http.post<FeaturedListing>(`/api/v1/owner/venues/${id}/featured/${listing.id}/cancel`, null));
      this.listings.update((list) => (list ?? []).map((l) => (l.id === updated.id ? updated : l)));
      this.confirmCancel.set(null);
      void this.loadQuotes(id, this.start(), this.days());
    }
    catch (error) {
      this.payingId.set(listing.id);
      this.payError.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  private async loadQuotes(id: string, startsOn: string, days: number): Promise<void> {
    const sequence = ++this.quoteSequence;
    this.quotes.set({});
    try {
      const quotes = await Promise.all(PLACEMENTS.map((p) => firstValueFrom(this.http.get<FeaturedQuote>(
        `/api/v1/owner/venues/${id}/featured/quote`, { params: { placement: p.value, startsOn, days } }))));
      if (sequence === this.quoteSequence) {
        this.quotes.set(Object.fromEntries(quotes.map((q) => [q.placement, q])));
      }
    }
    catch (error) {
      if (sequence === this.quoteSequence) {
        this.bookError.set(problemOf(error).message);
      }
    }
  }

  private async loadListings(id: string): Promise<void> {
    try {
      this.listings.set(await firstValueFrom(this.http.get<FeaturedListing[]>(`/api/v1/owner/venues/${id}/featured`)));
      this.error.set(null);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
