import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

import { DailyRow } from '../../core/api.types';
import { dayLabel, rupees } from '../../shared/format';

/** One column per day, or per week when the range is long enough that days would be slivers. */
interface Column {
  label: string;
  axisLabel: string;
  value: number;
  bookings: number;
  collected: number;
}

const WIDTH = 720;
const HEIGHT = 220;
const PAD = { top: 12, right: 8, bottom: 28, left: 56 };
const MAX_BAR = 24;
const GAP = 2;

/**
 * Booked value over the period: a single series, so no legend box; the card title names it. Hovering or
 * focusing a column shows its value first, then bookings and money collected. Inline SVG, theme tokens.
 */
@Component({
  selector: 'fs-daily-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="relative">
      <svg [attr.viewBox]="'0 0 ' + width + ' ' + height" class="block h-auto w-full" role="img"
        [attr.aria-label]="'Booked value by ' + (weekly() ? 'week' : 'day')">
        @for (tick of ticks(); track tick.value) {
          <line [attr.x1]="pad.left" [attr.x2]="width - pad.right" [attr.y1]="tick.y" [attr.y2]="tick.y"
            class="stroke-border" stroke-width="1" />
          <text [attr.x]="pad.left - 8" [attr.y]="tick.y" text-anchor="end" dominant-baseline="middle"
            class="tabular fill-subtle text-[11px]">{{ tick.label }}</text>
        }
        @for (column of columns(); track column.label; let i = $index) {
          <g tabindex="0" role="button" class="cursor-default outline-none"
            [attr.aria-label]="column.label + ': ' + money(column.value) + ', ' + column.bookings + ' bookings'"
            (pointerenter)="hover.set(i)" (pointerleave)="hover.set(null)" (focus)="hover.set(i)" (blur)="hover.set(null)">
            <!-- The hit area is the whole band, not just the painted bar -->
            <rect [attr.x]="bandX(i)" [attr.y]="pad.top" [attr.width]="band()" [attr.height]="plotHeight" fill="transparent" />
            @if (column.value > 0) {
              <path [attr.d]="barPath(i, column.value)" class="fill-viz" [class.opacity-75]="hover() !== null && hover() !== i" />
            }
          </g>
          @if (showLabel(i)) {
            <text [attr.x]="bandX(i) + band() / 2" [attr.y]="height - 8" text-anchor="middle" class="fill-subtle text-[11px]">{{ column.axisLabel }}</text>
          }
        }
        <line [attr.x1]="pad.left" [attr.x2]="width - pad.right" [attr.y1]="pad.top + plotHeight" [attr.y2]="pad.top + plotHeight"
          class="stroke-subtle" stroke-width="1" />
      </svg>

      @if (hover() !== null) {
        @if (columns()[hover()!]; as column) {
          <div class="pointer-events-none absolute top-0 z-10 min-w-36 -translate-x-1/2 rounded-md border border-border bg-surface px-3 py-2 shadow-lg"
            [style.left.%]="((bandX(hover()!) + band() / 2) / width) * 100" role="status">
            <p class="text-base font-semibold">{{ money(column.value) }}</p>
            <p class="text-xs text-muted">{{ column.label }}</p>
            <p class="mt-1 text-xs text-muted">{{ column.bookings }} bookings · {{ money(column.collected) }} collected</p>
          </div>
        }
      }
    </div>
  `,
})
export class DailyChart {
  readonly rows = input.required<DailyRow[]>();

  protected readonly width = WIDTH;

  protected readonly height = HEIGHT;

  protected readonly pad = PAD;

  protected readonly plotHeight = HEIGHT - PAD.top - PAD.bottom;

  protected readonly hover = signal<number | null>(null);

  protected readonly money = rupees;

  protected readonly weekly = computed(() => this.rows().length > 62);

  protected readonly columns = computed<Column[]>(() => {
    const rows = this.rows();
    if (!this.weekly()) {
      return rows.map((row) => ({
        label: dayLabel(row.date), axisLabel: shortDay(row.date), value: row.bookedValue, bookings: row.bookings,
        collected: row.collected,
      }));
    }
    const weeks: Column[] = [];
    for (let i = 0; i < rows.length; i += 7) {
      const week = rows.slice(i, i + 7);
      weeks.push({
        label: `Week of ${dayLabel(week[0].date)}`,
        axisLabel: shortDay(week[0].date),
        value: week.reduce((sum, row) => sum + row.bookedValue, 0),
        bookings: week.reduce((sum, row) => sum + row.bookings, 0),
        collected: week.reduce((sum, row) => sum + row.collected, 0),
      });
    }
    return weeks;
  });

  private readonly max = computed(() => niceMax(Math.max(0, ...this.columns().map((column) => column.value))));

  protected readonly ticks = computed(() => {
    const max = this.max();
    return [0, 0.25, 0.5, 0.75, 1].map((share) => ({
      value: max * share,
      y: PAD.top + this.plotHeight * (1 - share),
      label: compact(max * share),
    }));
  });

  protected readonly band = computed(() => (WIDTH - PAD.left - PAD.right) / Math.max(1, this.columns().length));

  protected bandX(index: number): number {
    return PAD.left + index * this.band();
  }

  /** Columns are capped at 24px and keep a 2px gap; the top corners are rounded, the baseline square. */
  protected barPath(index: number, value: number): string {
    const barWidth = Math.max(1, Math.min(MAX_BAR, this.band() - GAP));
    const x = this.bandX(index) + (this.band() - barWidth) / 2;
    const h = (value / this.max()) * this.plotHeight;
    const base = PAD.top + this.plotHeight;
    const y = base - h;
    const r = Math.min(4, barWidth / 2, h);
    return `M${x},${base} V${y + r} Q${x},${y} ${x + r},${y} H${x + barWidth - r} Q${x + barWidth},${y} ${x + barWidth},${y + r} V${base} Z`;
  }

  /** Enough axis labels to orient, never so many they collide. */
  protected showLabel(index: number): boolean {
    const count = this.columns().length;
    const every = Math.max(1, Math.ceil(count / 8));
    return index % every === 0;
  }
}

/**
 * A clean axis top: four equal steps of 1, 2, 2.5 or 5 times a power of ten, so the ticks read 0 / 1K / 2K
 * / 3K / 4K rather than 3.8K.
 */
function niceMax(value: number): number {
  if (value <= 0) {
    return 1000;
  }
  const rough = value / 4;
  const power = 10 ** Math.floor(Math.log10(rough));
  for (const factor of [1, 2, 2.5, 5, 10]) {
    if (factor * power >= rough) {
      return factor * power * 4;
    }
  }
  return 40 * power;
}

/** Axis money: 0, 2.5K, 1.2L (lakh), as people in Nepal read it. */
function compact(value: number): string {
  if (value >= 100_000) {
    return `${+(value / 100_000).toFixed(1)}L`;
  }
  if (value >= 1000) {
    return `${+(value / 1000).toFixed(1)}K`;
  }
  return String(Math.round(value));
}

function shortDay(date: string): string {
  const [, m, d] = date.split('-').map(Number);
  return `${d}/${m}`;
}
