import { HttpClient, HttpResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Check, Download, LucideAngularModule, Table } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import { problemOf } from '../../core/api-error';
import { DailyRow, PeakHourCell, TopCustomer, VenueReport } from '../../core/api.types';
import { VenueContext } from '../../core/venue-context';
import { addDays, dayLabel, methodLabel, phone, rupees, today } from '../../shared/format';
import { DailyChart } from './daily-chart';
import { PeakHeatmap } from './peak-heatmap';

type Preset = '7' | '30' | 'month' | 'last-month' | 'custom';

interface Range {
  from: string;
  to: string;
}

interface Data {
  summary: VenueReport;
  daily: DailyRow[];
  peak: PeakHourCell[];
  top: TopCustomer[];
}

const ZONE = 'Asia/Kathmandu';

/**
 * How the venue is doing over a period: the few numbers owners ask about first, then when it's busy, where
 * the money came from, and who the regulars are. Every section reads the same date range.
 */
@Component({
  selector: 'fs-reports',
  imports: [LucideAngularModule, DailyChart, PeakHeatmap],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './reports.html',
})
export class Reports {
  private readonly http = inject(HttpClient);

  private readonly router = inject(Router);

  protected readonly venues = inject(VenueContext);

  protected readonly icons = { Check, Download, Table };

  protected readonly fmt = { rupees, methodLabel, phone, dayLabel };

  protected readonly presets: { value: Preset; label: string }[] = [
    { value: '7', label: 'Last 7 days' },
    { value: '30', label: 'Last 30 days' },
    { value: 'month', label: 'This month' },
    { value: 'last-month', label: 'Last month' },
    { value: 'custom', label: 'Custom' },
  ];

  protected readonly preset = signal<Preset>('30');

  protected readonly range = signal<Range>(rangeFor('30'));

  protected readonly data = signal<Data | null>(null);

  protected readonly loading = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly showTable = signal(false);

  protected readonly exporting = signal(false);

  /** Where the money came from, biggest first, as shares of the largest for the bar lengths. */
  protected readonly methods = computed(() => {
    const byMethod = this.data()?.summary.money.collectedByMethod ?? {};
    const entries = Object.entries(byMethod).sort((a, b) => b[1] - a[1]);
    const max = Math.max(1, ...entries.map(([, amount]) => amount));
    return entries.map(([method, amount]) => ({ method, amount, share: (amount / max) * 100 }));
  });

  private sequence = 0;

  constructor() {
    const params = inject(ActivatedRoute).snapshot.queryParamMap;
    const from = params.get('from');
    const to = params.get('to');
    if (from && to) {
      this.preset.set('custom');
      this.range.set({ from, to });
    }
    effect(() => {
      const venue = this.venues.current();
      const range = this.range();
      if (venue) {
        void this.load(venue.venue.id, range);
      }
    });
  }

  protected choose(preset: Preset): void {
    this.preset.set(preset);
    if (preset !== 'custom') {
      this.apply(rangeFor(preset));
    }
  }

  protected setFrom(from: string): void {
    if (from) {
      this.apply({ from, to: this.range().to < from ? from : this.range().to });
    }
  }

  protected setTo(to: string): void {
    if (to) {
      this.apply({ from: this.range().from > to ? to : this.range().from, to });
    }
  }

  protected percent(value: number | null): string {
    return value === null ? '—' : `${value}%`;
  }

  protected source(key: string): string {
    return ({ APP: 'App', WALK_IN: 'Walk-in', PHONE: 'Phone', RECURRING: 'Weekly', TOURNAMENT: 'Tournament' } as Record<string, string>)[key] ?? key;
  }

  protected sources(summary: VenueReport): { key: string; count: number }[] {
    return Object.entries(summary.bookings.bySource)
      .map(([key, count]) => ({ key, count }))
      .sort((a, b) => b.count - a.count);
  }

  /** The bookings in the range as a CSV, for the venue's own accounts. */
  protected async exportCsv(): Promise<void> {
    const venue = this.venues.current();
    if (!venue || this.exporting()) {
      return;
    }
    this.exporting.set(true);
    try {
      const { from, to } = this.range();
      const response: HttpResponse<Blob> = await firstValueFrom(this.http.get(
        `/api/v1/owner/venues/${venue.venue.id}/reports/bookings.csv`,
        { params: { from, to }, responseType: 'blob', observe: 'response' }));
      const name = /filename="?([^";]+)"?/.exec(response.headers.get('Content-Disposition') ?? '')?.[1]
        ?? `bookings-${from}-to-${to}.csv`;
      const url = URL.createObjectURL(response.body!);
      const link = document.createElement('a');
      link.href = url;
      link.download = name;
      link.click();
      URL.revokeObjectURL(url);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
    finally {
      this.exporting.set(false);
    }
  }

  private apply(range: Range): void {
    this.range.set(range);
    void this.router.navigate([], {
      queryParams: this.preset() === 'custom' ? range : {}, replaceUrl: true,
    });
  }

  /** Keeps the last render on screen (dimmed) while the new range loads, so nothing jumps. */
  private async load(venueId: string, range: Range): Promise<void> {
    const sequence = ++this.sequence;
    this.loading.set(true);
    this.error.set(null);
    const base = `/api/v1/owner/venues/${venueId}/reports`;
    const params = { from: range.from, to: range.to };
    try {
      const [summary, daily, peak, top] = await Promise.all([
        firstValueFrom(this.http.get<VenueReport>(`${base}/summary`, { params })),
        firstValueFrom(this.http.get<DailyRow[]>(`${base}/daily`, { params })),
        firstValueFrom(this.http.get<PeakHourCell[]>(`${base}/peak-hours`, { params })),
        firstValueFrom(this.http.get<TopCustomer[]>(`${base}/top-customers`, { params: { ...params, limit: 10 } })),
      ]);
      if (sequence === this.sequence) {
        this.data.set({ summary, daily, peak, top });
      }
    }
    catch (error) {
      if (sequence === this.sequence) {
        this.error.set(problemOf(error).message);
      }
    }
    finally {
      if (sequence === this.sequence) {
        this.loading.set(false);
      }
    }
  }
}

function rangeFor(preset: Preset): Range {
  const now = today(ZONE);
  switch (preset) {
    case '7':
      return { from: addDays(now, -6), to: now };
    case 'month':
      return { from: `${now.slice(0, 8)}01`, to: now };
    case 'last-month': {
      const firstOfThisMonth = `${now.slice(0, 8)}01`;
      const lastOfPrevious = addDays(firstOfThisMonth, -1);
      return { from: `${lastOfPrevious.slice(0, 8)}01`, to: lastOfPrevious };
    }
    default:
      return { from: addDays(now, -29), to: now };
  }
}
