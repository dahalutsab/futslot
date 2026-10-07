import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CircleAlert, LucideAngularModule, Pencil, Plus } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import { problemOf } from '../../core/api-error';
import { Court, PricingRule, VenueDetail } from '../../core/api.types';
import { VenueContext } from '../../core/venue-context';
import { clock, rupees, today } from '../../shared/format';
import { CourtPanel, FORMATS, SURFACES } from './court-panel';
import { PriceDraft, PricePanel } from './price-panel';
import { Gap, gaps, LABELS, minutesOf, PriceCell, PriceDay, priceWeek, ruleWindow, span } from './pricing-model';
import { WEEK } from './venue-model';

const ZONE = 'Asia/Kathmandu';

/** Price shading, cheapest to dearest; literal so Tailwind generates them. */
const SHADES = ['bg-viz/10', 'bg-viz/20', 'bg-viz/30', 'bg-viz/45'];

interface CourtView {
  court: Court;
  week: PriceDay[];
  /** Every slot start in the week, for the grid's columns. */
  columns: number[];
  gaps: Gap[];
  shade: (price: number) => string;
}

type Panel =
  | { kind: 'court'; court: Court | null }
  | { kind: 'price'; court: Court; rule: PricingRule | null; draft: PriceDraft | null };

/**
 * The venue's courts and what each costs. A week of each court's slots shows the price players will see for
 * every hour, and calls out open hours with no price (players can't book those).
 */
@Component({
  selector: 'fs-courts',
  imports: [CourtPanel, LucideAngularModule, PricePanel, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './courts.html',
})
export class Courts {
  private readonly http = inject(HttpClient);

  protected readonly venues = inject(VenueContext);

  protected readonly icons = { CircleAlert, Pencil, Plus };

  protected readonly fmt = { clock, rupees, span, ruleWindow };

  protected readonly venue = signal<VenueDetail | null>(null);

  protected readonly error = signal<string | null>(null);

  protected readonly panel = signal<Panel | null>(null);

  protected readonly views = computed<CourtView[]>(() => {
    const venue = this.venue();
    if (!venue) {
      return [];
    }
    const from = today(venue.timezone || ZONE);
    return [...venue.courts].sort((a, b) => a.sortOrder - b.sortOrder || a.name.localeCompare(b.name)).map((court) => {
      const week = priceWeek(court, venue.operatingHours, from);
      const columns = [...new Set(week.flatMap((day) => day.cells.map((cell) => cell.start)))].sort((a, b) => a - b);
      const prices = [...new Set(week.flatMap((day) => day.cells.flatMap((cell) => (cell.rule ? [cell.rule.price] : []))))].sort((a, b) => a - b);
      const shade = (price: number) => {
        if (prices.length < 2) {
          return SHADES[0];
        }
        const rank = prices.indexOf(price) / (prices.length - 1);
        return SHADES[Math.round(rank * (SHADES.length - 1))];
      };
      return { court, week, columns, gaps: court.status === 'ACTIVE' ? gaps(week, court.slotDurationMinutes) : [], shade };
    });
  });

  constructor() {
    const venueId = computed(() => this.venues.current()?.venue.id ?? null);
    effect(() => {
      const id = venueId();
      if (id) {
        untracked(() => void this.load(id));
      }
    });
  }

  protected describe(court: Court): string {
    const length = court.slotDurationMinutes < 60 ? `${court.slotDurationMinutes} min`
      : `${court.slotDurationMinutes / 60} ${court.slotDurationMinutes === 60 ? 'hour' : 'hours'}`;
    return [FORMATS.find((f) => f.value === court.format)?.label, SURFACES.find((s) => s.value === court.surface)?.label,
      court.indoor ? 'Indoor' : 'Outdoor', `${length} bookings`].join(' · ');
  }

  protected cellAt(day: PriceDay, start: number): PriceCell | null {
    return day.cells.find((cell) => cell.start === start) ?? null;
  }

  protected days(rule: PricingRule): string {
    if (!rule.dayOfWeek) {
      return 'Every day';
    }
    return `${WEEK.find((d) => d.day === rule.dayOfWeek)?.label}s`;
  }

  protected label(rule: PricingRule): string {
    return LABELS[rule.label];
  }

  /** In use first, every-day before single days, then through the evening; hours before opening are after midnight. */
  protected sortedRules(court: Court): PricingRule[] {
    const hours = this.venue()?.operatingHours ?? [];
    const opens = (rule: PricingRule) => {
      const days = hours.filter((h) => !h.closed && h.openTime && (!rule.dayOfWeek || h.day === rule.dayOfWeek));
      return Math.min(...days.map((h) => minutesOf(h.openTime!)), 1440);
    };
    const key = (rule: PricingRule) => minutesOf(rule.startTime) + (minutesOf(rule.startTime) < opens(rule) ? 1440 : 0);
    return [...court.pricingRules].sort((a, b) => Number(b.active) - Number(a.active)
      || Number(a.dayOfWeek !== null) - Number(b.dayOfWeek !== null)
      || (a.dayOfWeek ?? '').localeCompare(b.dayOfWeek ?? '')
      || key(a) - key(b));
  }

  protected addCourt(): void {
    this.panel.set({ kind: 'court', court: null });
  }

  protected editCourt(court: Court): void {
    this.panel.set({ kind: 'court', court });
  }

  protected addPrice(court: Court, draft: PriceDraft | null = null): void {
    this.panel.set({ kind: 'price', court, rule: null, draft });
  }

  protected fillGap(court: Court, gap: Gap): void {
    this.addPrice(court, this.draftFor(court, gap));
  }

  /** A price for an unpriced stretch, suggesting what the hour before it costs. */
  private draftFor(court: Court, gap: Gap): PriceDraft {
    const day = this.views().find((v) => v.court.id === court.id)?.week.find((d) => d.date === gap.date);
    const before = day?.cells.filter((cell) => cell.start < gap.from && cell.rule).at(-1)?.rule;
    return { day: gap.weekday, from: gap.from, to: gap.to, price: before?.price ?? null, label: before?.label ?? null };
  }

  /** A priced cell edits its price; an empty one starts a price for that hour on that weekday. */
  protected openCell(view: CourtView, day: PriceDay, cell: PriceCell): void {
    if (cell.rule) {
      this.editPrice(view.court, cell.rule);
    }
    else {
      const gap = view.gaps.find((g) => g.date === day.date && cell.start >= g.from && cell.start < g.to);
      this.addPrice(view.court, gap ? this.draftFor(view.court, gap)
        : { day: null, from: cell.start, to: cell.start + view.court.slotDurationMinutes, price: null, label: null });
    }
  }

  protected editPrice(court: Court, rule: PricingRule): void {
    this.panel.set({ kind: 'price', court, rule, draft: null });
  }

  protected otherCourts(): Court[] {
    return this.venue()?.courts ?? [];
  }

  protected async saved(): Promise<void> {
    this.panel.set(null);
    const id = this.venue()?.id;
    if (id) {
      await this.load(id);
    }
  }

  protected retry(): void {
    const id = this.venues.current()?.venue.id;
    if (id) {
      void this.load(id);
    }
  }

  private async load(id: string): Promise<void> {
    try {
      this.venue.set(await firstValueFrom(this.http.get<VenueDetail>(`/api/v1/owner/venues/${id}`)));
      this.error.set(null);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
