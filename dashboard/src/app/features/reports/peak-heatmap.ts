import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

import { PeakHourCell } from '../../core/api.types';
import { clock } from '../../shared/format';

/** Sunday first: Saturday is the weekend day off in Nepal. */
const DAYS: { key: PeakHourCell['dayOfWeek']; label: string }[] = [
  { key: 'SUNDAY', label: 'Sun' }, { key: 'MONDAY', label: 'Mon' }, { key: 'TUESDAY', label: 'Tue' },
  { key: 'WEDNESDAY', label: 'Wed' }, { key: 'THURSDAY', label: 'Thu' }, { key: 'FRIDAY', label: 'Fri' },
  { key: 'SATURDAY', label: 'Sat' },
];

/** Spelled out so Tailwind generates them. */
const STEPS = ['bg-heat-1', 'bg-heat-2', 'bg-heat-3', 'bg-heat-4', 'bg-heat-5'];

/**
 * When the courts are busiest: weekdays by start hour, darker for more bookings (one hue, light to dark).
 * Empty cells are the plain surface. Each cell reports its count on hover and keyboard focus.
 */
@Component({
  selector: 'fs-peak-heatmap',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="overflow-x-auto">
      <table class="w-full border-separate [border-spacing:2px]">
        <caption class="sr-only">Played bookings by weekday and start hour</caption>
        <thead>
          <tr>
            <th scope="col" class="w-10"></th>
            @for (hour of hours(); track hour) {
              <th scope="col" class="tabular px-0 pb-1 text-center text-[11px] font-normal text-subtle">
                {{ hour % 3 === 0 ? short(hour) : '' }}
              </th>
            }
          </tr>
        </thead>
        <tbody>
          @for (day of days; track day.key) {
            <tr>
              <th scope="row" class="pr-2 text-left text-xs font-normal text-muted">{{ day.label }}</th>
              @for (hour of hours(); track hour) {
                <td class="p-0">
                  <div tabindex="0" class="h-7 min-w-6 rounded-[3px] outline-offset-1"
                    [class]="step(count(day.key, hour))"
                    [attr.aria-label]="day.label + ' ' + clockFor(hour) + ': ' + count(day.key, hour) + ' bookings'"
                    (pointerenter)="hover.set({ day: day.label, hour, count: count(day.key, hour) })" (pointerleave)="hover.set(null)"
                    (focus)="hover.set({ day: day.label, hour, count: count(day.key, hour) })" (blur)="hover.set(null)"></div>
                </td>
              }
            </tr>
          }
        </tbody>
      </table>
    </div>
    <div class="mt-3 flex flex-wrap items-center justify-between gap-3 text-xs text-muted">
      <p class="min-h-4" aria-live="polite">
        @if (hover(); as cell) {
          <span class="font-semibold text-text">{{ cell.count }} {{ cell.count === 1 ? 'booking' : 'bookings' }}</span>
          · {{ cell.day }} {{ clockFor(cell.hour) }}
        } @else {
          Hover or tab to a square for its count.
        }
      </p>
      <div class="flex items-center gap-1" aria-hidden="true">
        Fewer
        <span class="size-3 rounded-[2px] bg-surface-2"></span>
        @for (s of steps; track s) {
          <span class="size-3 rounded-[2px]" [class]="s"></span>
        }
        More
      </div>
    </div>
  `,
})
export class PeakHeatmap {
  readonly cells = input.required<PeakHourCell[]>();

  protected readonly days = DAYS;

  protected readonly steps = STEPS;

  protected readonly hover = signal<{ day: string; hour: number; count: number } | null>(null);

  private readonly counts = computed(() => {
    const map = new Map<string, number>();
    this.cells().forEach((cell) => map.set(`${cell.dayOfWeek}-${cell.hour}`, cell.bookings));
    return map;
  });

  private readonly max = computed(() => Math.max(1, ...this.cells().map((cell) => cell.bookings)));

  /** The hours anyone played, widened to at least 6 AM to 10 PM so quiet days still show the day's shape. */
  protected readonly hours = computed(() => {
    const played = this.cells().map((cell) => cell.hour);
    const first = Math.min(6, ...played);
    const last = Math.max(21, ...played);
    return Array.from({ length: last - first + 1 }, (_, i) => first + i);
  });

  protected count(day: string, hour: number): number {
    return this.counts().get(`${day}-${hour}`) ?? 0;
  }

  protected step(count: number): string {
    if (count === 0) {
      return 'bg-surface-2';
    }
    const index = Math.min(STEPS.length - 1, Math.ceil((count / this.max()) * STEPS.length) - 1);
    return STEPS[index];
  }

  protected clockFor(hour: number): string {
    return clock(hour * 60);
  }

  protected short(hour: number): string {
    return clock(hour * 60).replace(' ', '').toLowerCase();
  }
}
