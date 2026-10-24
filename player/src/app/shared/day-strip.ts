import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { l } from '../i18n/i18n';
import { shortDate, weekdayShort } from './format';

/**
 * The next few days to choose between, side by side: "Today", "Tomorrow", then weekday and date ("बिहि",
 * "२३ आश्विन" in Nepali). {@code days} are YYYY-MM-DD, the first of them today.
 */
@Component({
  selector: 'fs-day-strip',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="-mx-4 flex gap-2 overflow-x-auto px-4 pb-1 [scrollbar-width:none]" role="group" [attr.aria-label]="l().day">
      @for (d of days(); track d; let i = $index) {
        <button type="button" (click)="picked.emit(d)" [attr.aria-pressed]="selected() === d"
          class="flex h-16 min-w-16 shrink-0 flex-col items-center justify-center rounded-md border px-3 text-sm"
          [class]="selected() === d ? 'border-ink bg-ink text-ink-fg' : 'border-border bg-surface text-muted hover:text-text'">
          <span class="font-medium">{{ name(d, i) }}</span>
          <span class="tabular text-xs">{{ shortDate(d) }}</span>
        </button>
      }
    </div>
  `,
})
export class DayStrip {
  readonly days = input.required<string[]>();

  readonly selected = input<string | null>(null);

  readonly picked = output<string>();

  protected readonly l = l;

  protected readonly shortDate = shortDate;

  protected name(day: string, index: number): string {
    return index === 0 ? l().today : index === 1 ? l().tomorrow : weekdayShort(day);
  }
}
