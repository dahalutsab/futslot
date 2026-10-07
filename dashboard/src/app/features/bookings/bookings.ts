import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { CalendarDays, ChevronLeft, ChevronRight, LucideAngularModule, Search, X } from 'lucide-angular';

import { problemOf } from '../../core/api-error';
import { Booking } from '../../core/api.types';
import { VenueContext } from '../../core/venue-context';
import { Badges } from '../../layout/badges';
import { addDays, dayLabel, localClock, phone, rupees, today } from '../../shared/format';
import { StatusChip } from '../../shared/status-chip';
import { BookingPanel } from '../schedule/booking-panel';
import { ScheduleApi } from '../schedule/schedule-api';
import { BookingState, stateOf } from '../schedule/schedule-model';

type Filter = 'all' | 'unpaid' | 'held' | 'played' | 'cancelled';

const SOURCES: Record<string, string> = {
  APP: 'App', WALK_IN: 'Walk-in', PHONE: 'Phone', RECURRING: 'Weekly', TOURNAMENT: 'Tournament', ADMIN: 'FutSlot',
};

/**
 * Every booking for a day as a list, or found by reference, name or phone across all days. Opening one gives
 * the same panel as the Schedule.
 */
@Component({
  selector: 'fs-bookings',
  imports: [LucideAngularModule, StatusChip, BookingPanel],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './bookings.html',
})
export class Bookings {
  private readonly api = inject(ScheduleApi);

  private readonly route = inject(ActivatedRoute);

  private readonly router = inject(Router);

  private readonly badges = inject(Badges);

  protected readonly venues = inject(VenueContext);

  protected readonly icons = { CalendarDays, ChevronLeft, ChevronRight, Search, X };

  protected readonly fmt = { rupees, localClock, phone, dayLabel };

  protected readonly date = signal(this.route.snapshot.queryParamMap.get('date') ?? today('Asia/Kathmandu'));

  protected readonly query = signal(this.route.snapshot.queryParamMap.get('q') ?? '');

  protected readonly filter = signal<Filter>('all');

  protected readonly dayBookings = signal<Booking[] | null>(null);

  protected readonly found = signal<Booking[] | null>(null);

  protected readonly error = signal<string | null>(null);

  protected readonly selected = signal<Booking | null>(null);

  protected readonly searching = computed(() => this.query().trim().length >= 2);

  protected readonly filters: { value: Filter; label: string }[] = [
    { value: 'all', label: 'All' },
    { value: 'unpaid', label: 'Money due' },
    { value: 'held', label: 'Waiting for payment' },
    { value: 'played', label: 'Played' },
    { value: 'cancelled', label: 'Cancelled' },
  ];

  protected readonly shown = computed(() => {
    const list = (this.searching() ? this.found() : this.dayBookings()) ?? [];
    if (this.searching()) {
      return list;
    }
    switch (this.filter()) {
      case 'unpaid':
        return list.filter((b) => b.totalAmount > b.paidAmount && !['CANCELLED', 'EXPIRED'].includes(b.status));
      case 'held':
        return list.filter((b) => b.status === 'PENDING_PAYMENT');
      case 'played':
        return list.filter((b) => b.status === 'COMPLETED' || b.status === 'NO_SHOW');
      case 'cancelled':
        return list.filter((b) => b.status === 'CANCELLED' || b.status === 'EXPIRED');
      default:
        return list;
    }
  });

  protected readonly loaded = computed(() => (this.searching() ? this.found() : this.dayBookings()) !== null);

  private searchTimer: ReturnType<typeof setTimeout> | null = null;

  private searchSequence = 0;

  /** The query last searched for, so the URL echoing it back doesn't search again. */
  private searchedFor: string | null = null;

  constructor() {
    effect(() => {
      const venue = this.venues.current();
      const date = this.date();
      if (venue) {
        void this.loadDay(venue.venue.id, date);
      }
    });
    // The URL can change while this screen is open (search from anywhere with Ctrl+K): follow it.
    // ?open=<id> opens that booking once the search has found it.
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const q = params.get('q') ?? '';
      const open = params.get('open');
      if (q !== this.searchedFor || open) {
        this.query.set(q);
        void this.runSearch(open);
      }
    });
    inject(DestroyRef).onDestroy(() => this.searchTimer && clearTimeout(this.searchTimer));
  }

  protected typed(value: string): void {
    this.query.set(value);
    if (this.searchTimer) {
      clearTimeout(this.searchTimer);
    }
    this.searchTimer = setTimeout(() => void this.runSearch(), 300);
  }

  protected clearSearch(): void {
    this.typed('');
  }

  protected shift(days: number): void {
    this.setDate(addDays(this.date(), days));
  }

  protected setDate(date: string): void {
    if (date) {
      this.date.set(date);
      this.syncUrl();
    }
  }

  protected state(booking: Booking): BookingState | null {
    return stateOf(booking);
  }

  protected source(booking: Booking): string {
    return SOURCES[booking.source] ?? booking.source;
  }

  protected due(booking: Booking): number {
    return ['CANCELLED', 'EXPIRED'].includes(booking.status) ? 0 : Math.max(0, booking.totalAmount - booking.paidAmount);
  }

  protected changed(): void {
    this.selected.set(null);
    const venue = this.venues.current();
    if (venue) {
      void this.loadDay(venue.venue.id, this.date());
      if (this.searching()) {
        void this.runSearch();
      }
    }
    void this.badges.refresh();
  }

  private async runSearch(open: string | null = null): Promise<void> {
    this.searchedFor = this.query().trim();
    this.syncUrl();
    const venue = this.venues.current();
    if (!venue || !this.searching()) {
      this.found.set(null);
      return;
    }
    const sequence = ++this.searchSequence;
    try {
      const results = await this.api.search(venue.venue.id, this.query().trim());
      if (sequence === this.searchSequence) {
        this.found.set(results);
        this.error.set(null);
        if (open) {
          this.selected.set(results.find((b) => b.id === open) ?? null);
        }
      }
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }

  private async loadDay(venueId: string, date: string): Promise<void> {
    try {
      this.dayBookings.set((await this.api.bookings(venueId, date)).slice().sort((a, b) => a.startsAt.localeCompare(b.startsAt)));
      this.error.set(null);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }

  private syncUrl(): void {
    const q = this.query().trim();
    void this.router.navigate([], { queryParams: { date: this.date(), q: q || null }, replaceUrl: true });
  }
}
