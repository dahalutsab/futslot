import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router, RouterLink } from '@angular/router';
import { Heart, LucideAngularModule, MapPin, MessageSquare, Phone, Star } from 'lucide-angular';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { Session } from '../../core/session';
import { Availability, CourtSlots, RatingSummary, Review, Slot, VenueDetail } from '../../core/api.types';
import { digits, l } from '../../i18n/i18n';
import { hoursLabel, policyLines, week } from '../../shared/booking-model';
import { VenuePlans } from './plans';
import { addDays, ago, dayLabel, localClock, phone, rupees, shortDate, today, weekdayShort } from '../../shared/format';
import { amenityLabel } from '../../shared/labels';

function slotKey(courtId: string, startsAt: string): string {
  return `${courtId}@${Date.parse(startsAt)}`;
}

/** How many days the day strip offers, at most. */
const DAYS_SHOWN = 14;

/** One venue: photos and details, free slots by day and court, the booking rules, and reviews. */
@Component({
  selector: 'fs-venue',
  imports: [RouterLink, LucideAngularModule, VenuePlans],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './venue.html',
})
export class VenuePage {
  protected readonly l = l;

  private readonly api = inject(Api);

  private readonly router = inject(Router);

  private readonly title = inject(Title);

  protected readonly session = inject(Session);

  readonly slug = input.required<string>();

  /** YYYY-MM-DD from the address, so a shared link opens the same day. */
  readonly date = input<string | undefined>();

  protected readonly icons = { Heart, MapPin, MessageSquare, Phone, Star };

  protected readonly fmt = { rupees, localClock, dayLabel, shortDate, phone, ago };

  protected readonly digits = digits;

  protected readonly amenityLabel = amenityLabel;

  protected readonly venue = signal<VenueDetail | null>(null);

  protected readonly availability = signal<Availability | null>(null);

  protected readonly rating = signal<RatingSummary | null>(null);

  protected readonly reviews = signal<Review[]>([]);

  protected readonly error = signal<string | null>(null);

  protected readonly slotsError = signal<string | null>(null);

  protected readonly photo = signal(0);

  protected readonly saved = signal(false);

  /** Booked slots the player chose to wait for here, by court and start. */
  protected readonly waiting = signal<Set<string>>(new Set());

  /** The booked slot whose "wait for it" choice is open. */
  protected readonly asking = signal<string | null>(null);

  protected readonly waitNote = signal<{ key: string; text: string; ok: boolean } | null>(null);

  protected readonly days = computed(() => {
    const venue = this.venue();
    if (!venue) {
      return [];
    }
    const first = today(venue.timezone);
    const count = Math.min(DAYS_SHOWN, venue.policy.maxAdvanceBookingDays + 1);
    return Array.from({ length: count }, (_, i) => addDays(first, i));
  });

  protected readonly day = computed(() => {
    const days = this.days();
    const asked = this.date();
    return asked && days.includes(asked) ? asked : days[0];
  });

  protected readonly rules = computed(() => {
    const venue = this.venue();
    return venue ? policyLines(venue.policy) : [];
  });

  protected readonly hours = computed(() => {
    const venue = this.venue();
    return week().map((day) => ({ ...day, hours: hoursLabel(venue?.operatingHours.find((h) => h.day === day.day)) }));
  });

  protected readonly todayLabel = computed(() => {
    const venue = this.venue();
    return venue ? week()[new Date(`${today(venue.timezone)}T00:00:00Z`).getUTCDay()].day : null;
  });

  constructor() {
    effect(() => {
      const slug = this.slug();
      untracked(() => void this.load(slug));
    });
    effect(() => {
      const venue = this.venue();
      const day = this.day();
      if (venue && day) {
        untracked(() => void this.loadSlots(venue.slug, day));
      }
    });
  }

  protected pickDay(day: string): void {
    void this.router.navigate([], { queryParams: { date: day }, replaceUrl: true });
  }

  protected dayName(day: string, index: number): string {
    return index === 0 ? l().today : index === 1 ? l().tomorrow : weekdayShort(day);
  }

  /** Slots that haven't started: past ones only clutter today's list. */
  protected notPast(court: CourtSlots): Slot[] {
    return court.slots.filter((slot) => slot.status !== 'PAST');
  }

  protected free(court: CourtSlots): number {
    return court.slots.filter((slot) => slot.status === 'AVAILABLE').length;
  }

  protected book(court: CourtSlots, slot: Slot): void {
    void this.router.navigate(['/venues', this.slug(), 'book'], { queryParams: { court: court.courtId, start: slot.startsAt } });
  }

  /** By instant, since the waitlist and availability may write the same start with different offsets. */
  protected key(court: CourtSlots, slot: Slot): string {
    return slotKey(court.courtId, slot.startsAt);
  }

  protected async toggleSaved(): Promise<void> {
    const venue = this.venue();
    if (!venue) {
      return;
    }
    if (!this.session.signedIn()) {
      void this.router.navigate(['/login'], { queryParams: { next: this.router.url } });
      return;
    }
    const on = !this.saved();
    this.saved.set(on);
    try {
      await this.api.favourite(venue.id, on);
    }
    catch {
      this.saved.set(!on);
    }
  }

  protected message(): void {
    const venue = this.venue();
    if (venue) {
      void this.router.navigate(['/messages'], { queryParams: { venue: venue.id, name: venue.name } });
    }
  }

  protected async wait(court: CourtSlots, slot: Slot): Promise<void> {
    const key = this.key(court, slot);
    if (!this.session.signedIn()) {
      void this.router.navigate(['/login'], { queryParams: { next: this.router.url } });
      return;
    }
    try {
      await this.api.joinWaitlist(court.courtId, slot.startsAt);
      this.waiting.update((set) => new Set(set).add(key));
      this.waitNote.set({ key, ok: true, text: l().weWillTellIfFree });
    }
    catch (error) {
      this.waitNote.set({ key, ok: false, text: problemOf(error).message });
    }
    this.asking.set(null);
  }

  protected stars(rating: number): string {
    return '★'.repeat(rating) + '☆'.repeat(5 - rating);
  }

  protected async load(slug: string): Promise<void> {
    this.error.set(null);
    this.venue.set(null);
    try {
      const venue = await this.api.venue(slug);
      this.venue.set(venue);
      this.title.setTitle(l().venueTitle(venue.name));
      const [rating, reviews] = await Promise.all([this.api.ratingSummary(slug), this.api.reviews(slug)]);
      this.rating.set(rating);
      this.reviews.set(reviews.content);
      if (this.session.signedIn()) {
        const [favourites, waitlist] = await Promise.all([this.api.favourites(), this.api.waitlist()]);
        this.saved.set(favourites.some((f) => f.id === venue.id));
        this.waiting.set(new Set(waitlist.filter((w) => w.venueId === venue.id && (w.status === 'WAITING' || w.status === 'NOTIFIED'))
          .map((w) => slotKey(w.courtId, w.startsAt))));
      }
    }
    catch (error) {
      if (!this.venue()) {
        this.error.set(problemOf(error).message);
      }
    }
  }

  private async loadSlots(slug: string, day: string): Promise<void> {
    this.availability.set(null);
    this.slotsError.set(null);
    try {
      this.availability.set(await this.api.availability(slug, day));
    }
    catch (error) {
      this.slotsError.set(problemOf(error).message);
    }
  }
}
