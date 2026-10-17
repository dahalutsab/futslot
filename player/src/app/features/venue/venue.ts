import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule, MapPin, Phone, Star } from 'lucide-angular';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { Availability, CourtSlots, RatingSummary, Review, Slot, VenueDetail } from '../../core/api.types';
import { hoursLabel, policyLines, WEEK } from '../../shared/booking-model';
import { addDays, ago, dayLabel, localClock, phone, rupees, today } from '../../shared/format';

/** How many days the day strip offers, at most. */
const DAYS_SHOWN = 14;

/** One venue: photos and details, free slots by day and court, the booking rules, and reviews. */
@Component({
  selector: 'fs-venue',
  imports: [RouterLink, LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './venue.html',
})
export class VenuePage {
  private readonly api = inject(Api);

  private readonly router = inject(Router);

  private readonly title = inject(Title);

  readonly slug = input.required<string>();

  /** YYYY-MM-DD from the address, so a shared link opens the same day. */
  readonly date = input<string | undefined>();

  protected readonly icons = { MapPin, Phone, Star };

  protected readonly fmt = { rupees, localClock, dayLabel, phone, ago };

  protected readonly venue = signal<VenueDetail | null>(null);

  protected readonly availability = signal<Availability | null>(null);

  protected readonly rating = signal<RatingSummary | null>(null);

  protected readonly reviews = signal<Review[]>([]);

  protected readonly error = signal<string | null>(null);

  protected readonly slotsError = signal<string | null>(null);

  protected readonly photo = signal(0);

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
    return WEEK.map((day) => ({ ...day, hours: hoursLabel(venue?.operatingHours.find((h) => h.day === day.day)) }));
  });

  protected readonly todayLabel = computed(() => {
    const venue = this.venue();
    return venue ? WEEK[new Date(`${today(venue.timezone)}T00:00:00Z`).getUTCDay()].day : null;
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
    return index === 0 ? 'Today' : index === 1 ? 'Tomorrow' : dayLabel(day).split(' ')[0];
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

  protected stars(rating: number): string {
    return '★'.repeat(rating) + '☆'.repeat(5 - rating);
  }

  protected async load(slug: string): Promise<void> {
    this.error.set(null);
    this.venue.set(null);
    try {
      const venue = await this.api.venue(slug);
      this.venue.set(venue);
      this.title.setTitle(`${venue.name} · FutSlot`);
      const [rating, reviews] = await Promise.all([this.api.ratingSummary(slug), this.api.reviews(slug)]);
      this.rating.set(rating);
      this.reviews.set(reviews.content);
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
