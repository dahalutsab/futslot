import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  Lock,
  LucideAngularModule,
  Plus,
  RefreshCw,
  Trophy,
} from 'lucide-angular';

import { problemOf } from '../../core/api-error';
import { Booking, CourtBlock, Slot } from '../../core/api.types';
import { VenueContext } from '../../core/venue-context';
import { Badges } from '../../layout/badges';
import { addDays, clock, dayLabel, minutesInto, rupees, today } from '../../shared/format';
import { ChipState, stateIcon, stateLabel } from '../../shared/status-chip';
import { BlockPanel } from './block-panel';
import { BookingPanel } from './booking-panel';
import { ScheduleApi } from './schedule-api';
import { BookingState, buildDay, DayModel, ScheduleItem } from './schedule-model';
import { WalkInPanel } from './walk-in-panel';

/** Nepal; the API reports each venue's own zone with its availability. */
const DEFAULT_ZONE = 'Asia/Kathmandu';

/** Pixels per minute in the grid: an hour is 72px, tall enough for two lines and a comfortable tap. */
const PX_PER_MINUTE = 1.2;

type Selection =
  | { kind: 'free'; courtId: string; courtName: string; slot: Slot }
  | { kind: 'booking'; booking: Booking }
  | { kind: 'block'; block: CourtBlock; courtName: string };

/** Spelled out in full so Tailwind generates them (it can't see classes built from strings). */
const ICON_CLASSES: Record<BookingState, string> = {
  confirmed: 'text-confirmed-fg',
  held: 'text-held-fg',
  verify: 'text-verify-fg',
  completed: 'text-muted',
  noshow: 'text-noshow-fg',
};

const LEGEND: { state: ChipState; swatch: string }[] = [
  { state: 'confirmed', swatch: 'bg-confirmed-line' },
  { state: 'held', swatch: 'bg-held-line' },
  { state: 'verify', swatch: 'bg-verify-line' },
  { state: 'completed', swatch: 'bg-border' },
  { state: 'noshow', swatch: 'bg-noshow-line' },
  { state: 'blocked', swatch: 'hatched' },
  { state: 'tournament', swatch: 'bg-tournament-line' },
];

const STATE_CLASSES: Record<BookingState, string> = {
  confirmed: 'bg-confirmed-bg border-confirmed-line text-confirmed-fg',
  held: 'bg-held-bg border-held-line text-held-fg',
  verify: 'bg-verify-bg border-verify-line text-verify-fg',
  completed: 'bg-surface-2 border-border text-muted',
  noshow: 'bg-noshow-bg border-noshow-line text-noshow-fg',
};

/**
 * The home screen: every court for one day. Free slots open a walk-in booking, bookings and blocks open
 * their details. Refreshes every minute so a screen left open at the counter stays current.
 */
@Component({
  selector: 'fs-schedule',
  imports: [LucideAngularModule, WalkInPanel, BookingPanel, BlockPanel],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './schedule.html',
})
export class Schedule {
  private readonly api = inject(ScheduleApi);

  private readonly route = inject(ActivatedRoute);

  private readonly router = inject(Router);

  private readonly badges = inject(Badges);

  protected readonly venues = inject(VenueContext);

  protected readonly icons = { CalendarDays, ChevronLeft, ChevronRight, Lock, Plus, RefreshCw, Trophy };

  protected readonly fmt = { rupees, clock, dayLabel };

  protected readonly pxPerMinute = PX_PER_MINUTE;

  protected readonly legend = LEGEND;

  protected readonly day = signal<DayModel | null>(null);

  protected readonly loading = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly selection = signal<Selection | null>(null);

  /** Which court phones show (one at a time). */
  protected readonly phoneCourt = signal(0);

  private readonly now = signal(new Date());

  protected readonly date = signal(this.route.snapshot.queryParamMap.get('date') ?? today(DEFAULT_ZONE));

  protected readonly isToday = computed(() => this.date() === today(this.day()?.timeZone ?? DEFAULT_ZONE, this.now()));

  protected readonly hours = computed(() => {
    const day = this.day();
    if (!day) {
      return [];
    }
    const hours: number[] = [];
    for (let minute = day.start; minute < day.end; minute += 60) {
      hours.push(minute);
    }
    return hours;
  });

  /** Where the red "now" line sits, or null when it's not today or outside opening hours. */
  protected readonly nowOffset = computed(() => {
    const day = this.day();
    if (!day || !this.isToday()) {
      return null;
    }
    const minutes = minutesInto(day.date, this.now().toISOString(), day.timeZone);
    return minutes >= day.start && minutes <= day.end ? (minutes - day.start) * PX_PER_MINUTE : null;
  });

  protected readonly canBook = computed(() => this.venues.can('MANAGE_BOOKINGS'));

  protected readonly canBlock = computed(() => this.venues.can('MANAGE_BLOCKS'));

  private loadSequence = 0;

  constructor() {
    effect(() => {
      const venue = this.venues.current();
      const date = this.date();
      if (venue) {
        void this.load(venue.venue.id, date);
      }
    });
    const tick = setInterval(() => {
      this.now.set(new Date());
      const venue = this.venues.current();
      if (venue && !this.selection()) {
        void this.load(venue.venue.id, this.date(), true);
      }
    }, 60_000);
    inject(DestroyRef).onDestroy(() => clearInterval(tick));
  }

  protected shift(days: number): void {
    this.goTo(addDays(this.date(), days));
  }

  protected goTo(date: string): void {
    if (!date) {
      return;
    }
    this.date.set(date);
    this.phoneCourt.set(0);
    void this.router.navigate([], { queryParams: { date }, replaceUrl: true });
  }

  protected goToday(): void {
    this.goTo(today(this.day()?.timeZone ?? DEFAULT_ZONE));
  }

  protected refresh(): void {
    const venue = this.venues.current();
    if (venue) {
      void this.load(venue.venue.id, this.date());
    }
  }

  /** After a booking, payment or block changes: reload the day and the nav badges. */
  protected changed(): void {
    this.selection.set(null);
    this.refresh();
    void this.badges.refresh();
  }

  protected open(item: ScheduleItem, courtId: string, courtName: string): void {
    switch (item.kind) {
      case 'free':
        if (this.canBook() || this.canBlock()) {
          this.selection.set({ kind: 'free', courtId, courtName, slot: item.slot });
        }
        break;
      case 'booking':
        this.selection.set({ kind: 'booking', booking: item.booking });
        break;
      case 'block':
        this.selection.set({ kind: 'block', block: item.block, courtName });
        break;
      case 'past':
        break;
    }
  }

  protected top(item: { start: number }): number {
    return (item.start - (this.day()?.start ?? 0)) * PX_PER_MINUTE;
  }

  protected height(item: { start: number; end: number }): number {
    return (item.end - item.start) * PX_PER_MINUTE;
  }

  protected bookingClasses(state: BookingState): string {
    return STATE_CLASSES[state];
  }

  protected iconClass(state: BookingState): string {
    return ICON_CLASSES[state];
  }

  protected chipIcon(state: ChipState) {
    return stateIcon(state);
  }

  protected chipLabel(state: ChipState): string {
    return stateLabel(state);
  }

  /** "Rs 400 due", "Paid", or nothing when there's nothing to pay. */
  protected paymentNote(booking: Booking): string | null {
    const due = booking.totalAmount - booking.paidAmount;
    if (booking.totalAmount === 0) {
      return null;
    }
    return due > 0 ? `${rupees(due)} due` : 'Paid';
  }

  protected range(item: { start: number; end: number }): string {
    return `${clock(item.start)} – ${clock(item.end)}`;
  }

  protected trackItem(item: ScheduleItem): string {
    switch (item.kind) {
      case 'booking':
        return 'b' + item.booking.id;
      case 'block':
        return 'k' + item.block.id;
      default:
        return item.kind + item.start;
    }
  }

  private async load(venueId: string, date: string, quiet = false): Promise<void> {
    const sequence = ++this.loadSequence;
    if (!quiet) {
      this.loading.set(true);
      this.error.set(null);
    }
    try {
      const [availability, bookings] = await Promise.all([
        this.api.availability(venueId, date),
        this.api.bookings(venueId, date),
      ]);
      const blocks = await this.api.blocks(venueId, availability.courts.map((court) => court.courtId), date);
      if (sequence === this.loadSequence) {
        this.day.set(buildDay(availability, bookings, blocks, new Date()));
        this.error.set(null);
      }
    }
    catch (error) {
      if (sequence === this.loadSequence && !quiet) {
        this.error.set(problemOf(error).message);
      }
    }
    finally {
      if (sequence === this.loadSequence) {
        this.loading.set(false);
      }
    }
  }
}
