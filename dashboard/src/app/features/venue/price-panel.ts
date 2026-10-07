import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { LoaderCircle, LucideAngularModule } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import { problemOf } from '../../core/api-error';
import { Court, PriceLabel, PricingRule, Weekday } from '../../core/api.types';
import { rupees } from '../../shared/format';
import { Sheet } from '../../shared/sheet';
import { WEEK } from './venue-model';
import { LABELS, minutesOf, windows } from './pricing-model';

/** Where a new price starts from, e.g. an unpriced stretch of the week. */
export interface PriceDraft {
  day: Weekday | null;
  /** Minutes past the operating day's midnight; past 1440 means after midnight. */
  from: number;
  to: number;
  /** A starting price, e.g. what the hour before costs. */
  price: number | null;
  label: PriceLabel | null;
}

function hhmm(minutes: number): string {
  const inDay = minutes % 1440;
  return `${String(Math.floor(inDay / 60)).padStart(2, '0')}:${String(inDay % 60).padStart(2, '0')}`;
}

/**
 * Adds prices to a court for some days and hours, or changes or removes one. Late-night prices (ending after
 * midnight) are saved as two prices split at midnight, because each price covers part of a single day.
 */
@Component({
  selector: 'fs-price-panel',
  imports: [LucideAngularModule, Sheet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <fs-sheet [title]="rule() ? 'Edit price' : 'Add a price'" [subtitle]="court().name" (closed)="closed.emit()">
      <form id="price-form" class="space-y-4" (submit)="$event.preventDefault(); save()">
        <fieldset>
          <legend class="mb-1.5 font-medium">Days</legend>
          <div class="flex flex-wrap gap-2">
            <button type="button" [attr.aria-pressed]="days().length === 0" (click)="days.set([])"
              class="h-11 rounded-full border px-4 font-medium" [class]="days().length === 0 ? 'border-ink bg-ink text-ink-fg' : 'border-border text-muted'">
              Every day
            </button>
            @for (day of week; track day.day) {
              <button type="button" [attr.aria-pressed]="days().includes(day.day)" (click)="toggleDay(day.day)" [attr.aria-label]="day.label"
                class="h-11 min-w-11 rounded-full border px-3 font-medium" [class]="days().includes(day.day) ? 'border-ink bg-ink text-ink-fg' : 'border-border text-muted'">
                {{ day.short }}
              </button>
            }
          </div>
        </fieldset>
        <div>
          <span class="mb-1.5 block font-medium" id="price-hours">Hours</span>
          <div class="flex items-center gap-2" role="group" aria-labelledby="price-hours">
            <input type="time" step="900" [value]="from()" (input)="from.set($any($event.target).value)" aria-label="From"
              class="tabular h-12 flex-1 rounded-lg border border-border bg-surface px-3 text-base" />
            <span class="text-muted">to</span>
            <input type="time" step="900" [value]="to()" (input)="to.set($any($event.target).value)" aria-label="To"
              class="tabular h-12 flex-1 rounded-lg border border-border bg-surface px-3 text-base" />
          </div>
          @if (pastMidnight()) {
            <p class="mt-1.5 text-sm text-muted">Runs past midnight. Late hours count as the night they started, so Friday 11 PM – 1 AM is late Friday night.</p>
          }
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label for="price-amount" class="mb-1.5 block font-medium">Price per booking</label>
            <div class="relative">
              <span class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted">Rs</span>
              <input id="price-amount" type="number" inputmode="numeric" min="0" step="50" [value]="price()" (input)="price.set($any($event.target).value)"
                class="tabular h-12 w-full rounded-lg border border-border bg-surface pl-10 pr-3 text-base" />
            </div>
          </div>
          <div>
            <label for="price-label" class="mb-1.5 block font-medium">Shown as</label>
            <select id="price-label" (change)="label.set($any($event.target).value)" class="h-12 w-full rounded-lg border border-border bg-surface px-3 text-base">
              @for (option of labels; track option.value) {
                <option [value]="option.value" [selected]="label() === option.value">{{ option.label }}</option>
              }
            </select>
          </div>
        </div>

        <details class="rounded-lg border border-border" [open]="moreOpen()">
          <summary class="flex h-11 cursor-pointer items-center px-3 font-medium">More options</summary>
          <div class="space-y-4 border-t border-border p-3">
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label for="price-from-date" class="mb-1.5 block text-sm font-medium">Starting</label>
                <input id="price-from-date" type="date" [value]="validFrom()" (input)="validFrom.set($any($event.target).value)"
                  class="h-11 w-full rounded-lg border border-border bg-surface px-2 text-base" />
              </div>
              <div>
                <label for="price-to-date" class="mb-1.5 block text-sm font-medium">Until</label>
                <input id="price-to-date" type="date" [value]="validTo()" (input)="validTo.set($any($event.target).value)"
                  class="h-11 w-full rounded-lg border border-border bg-surface px-2 text-base" />
              </div>
            </div>
            <p class="-mt-2 text-sm text-subtle">Leave empty to apply from now on, e.g. set dates for a Dashain offer.</p>
            <div>
              <label for="price-priority" class="mb-1.5 block text-sm font-medium">Priority</label>
              <input id="price-priority" type="number" inputmode="numeric" [value]="priority()" (input)="priority.set(+$any($event.target).value)"
                class="tabular h-11 w-24 rounded-lg border border-border bg-surface px-3 text-base" />
              <p class="mt-1 text-sm text-subtle">Where prices overlap, the higher number wins. At equal priority a single day beats "every day".</p>
            </div>
            @if (rule()) {
              <label class="flex cursor-pointer items-center gap-3">
                <input type="checkbox" class="size-4 accent-current" [checked]="active()" (change)="active.set(!active())" />
                <span class="font-medium">In use <span class="font-normal text-subtle">(untick to pause it without deleting)</span></span>
              </label>
            }
          </div>
        </details>
      </form>

      @if (rule()) {
        <div class="mt-6 border-t border-border pt-4">
          @if (confirmDelete()) {
            <p class="font-medium">Delete this price?</p>
            <p class="text-sm text-muted">Hours it covered become unbookable unless another price covers them.</p>
            <div class="mt-3 flex gap-2">
              <button type="button" (click)="remove()" [disabled]="busy()" class="h-11 rounded-lg bg-danger px-4 font-medium text-surface disabled:opacity-60">Delete</button>
              <button type="button" (click)="confirmDelete.set(false)" class="h-11 rounded-lg border border-border px-4 font-medium">Keep it</button>
            </div>
          } @else {
            <button type="button" (click)="confirmDelete.set(true)" class="h-11 rounded-lg px-1 font-medium text-danger hover:underline">Delete price</button>
          }
        </div>
      }

      @if (error(); as message) {
        <p class="mt-4 rounded-lg bg-danger-bg px-3 py-2 text-danger" role="alert">{{ message }}</p>
      }

      <div sheet-footer class="border-t border-border px-5 py-4 pb-[calc(1rem+env(safe-area-inset-bottom))]">
        <button type="submit" form="price-form" [disabled]="busy()"
          class="flex h-12 w-full items-center justify-center gap-2 rounded-lg bg-ink font-medium text-ink-fg disabled:opacity-60">
          @if (busy()) {
            <lucide-icon [img]="spinner" [size]="18" class="animate-spin" />
          }
          {{ rule() ? 'Save' : 'Add price' }}
        </button>
      </div>
    </fs-sheet>
  `,
})
export class PricePanel implements OnInit {
  private readonly http = inject(HttpClient);

  readonly venueId = input.required<string>();

  readonly court = input.required<Court>();

  /** The price to edit, or null to add one. */
  readonly rule = input<PricingRule | null>(null);

  readonly draft = input<PriceDraft | null>(null);

  readonly done = output<void>();

  readonly closed = output<void>();

  protected readonly week = WEEK;

  protected readonly labels = (Object.keys(LABELS) as PriceLabel[]).map((value) => ({ value, label: LABELS[value] }));

  protected readonly spinner = LoaderCircle;

  /** Empty means every day. */
  protected readonly days = signal<Weekday[]>([]);

  protected readonly from = signal('06:00');

  protected readonly to = signal('22:00');

  protected readonly price = signal('');

  protected readonly label = signal<PriceLabel>('STANDARD');

  protected readonly validFrom = signal('');

  protected readonly validTo = signal('');

  protected readonly priority = signal(0);

  protected readonly active = signal(true);

  protected readonly moreOpen = signal(false);

  protected readonly confirmDelete = signal(false);

  protected readonly busy = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly pastMidnight = computed(() => !!this.from() && !!this.to() && this.to() !== '00:00' && this.to() <= this.from());

  ngOnInit(): void {
    const rule = this.rule();
    const draft = this.draft();
    if (rule) {
      this.days.set(rule.dayOfWeek ? [rule.dayOfWeek] : []);
      this.from.set(rule.startTime.slice(0, 5));
      this.to.set(minutesOf(rule.endTime) >= 1439 ? '00:00' : rule.endTime.slice(0, 5));
      this.price.set(String(rule.price));
      this.label.set(rule.label);
      this.validFrom.set(rule.validFrom ?? '');
      this.validTo.set(rule.validTo ?? '');
      this.priority.set(rule.priority);
      this.active.set(rule.active);
      this.moreOpen.set(!!rule.validFrom || !!rule.validTo || rule.priority !== 0 || !rule.active);
    }
    else if (draft) {
      this.days.set(draft.day ? [draft.day] : []);
      this.from.set(hhmm(draft.from));
      this.to.set(hhmm(draft.to));
      this.price.set(draft.price != null ? String(draft.price) : '');
      this.label.set(draft.label ?? 'STANDARD');
    }
  }

  protected toggleDay(day: Weekday): void {
    // An existing price covers one day (or every day), so editing picks one
    if (this.rule()) {
      this.days.set([day]);
      return;
    }
    this.days.update((days) => (days.includes(day) ? days.filter((d) => d !== day) : [...days, day]));
  }

  protected async save(): Promise<void> {
    if (this.busy()) {
      return;
    }
    const problem = this.problem();
    if (problem) {
      this.error.set(problem);
      return;
    }
    const spans = windows(this.from(), this.to());
    const rule = this.rule();
    if (rule && spans.length > 1) {
      this.error.set('A saved price can\'t run past midnight. End this one at 12 AM and add another from 12 AM.');
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    const base = `/api/v1/owner/venues/${this.venueId()}/courts/${this.court().id}/pricing-rules`;
    const common = {
      label: this.label(),
      price: Number(this.price()),
      priority: this.priority(),
      validFrom: this.validFrom() || null,
      validTo: this.validTo() || null,
      active: this.active(),
    };
    try {
      if (rule) {
        await firstValueFrom(this.http.put(`${base}/${rule.id}`, { ...common, ...spans[0], dayOfWeek: this.days()[0] ?? null }));
      }
      else {
        const days: (Weekday | null)[] = this.days().length ? this.days() : [null];
        for (const dayOfWeek of days) {
          for (const window of spans) {
            await firstValueFrom(this.http.post(base, { ...common, ...window, dayOfWeek }));
          }
        }
      }
      this.done.emit();
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  protected async remove(): Promise<void> {
    const rule = this.rule();
    if (!rule || this.busy()) {
      return;
    }
    this.busy.set(true);
    try {
      await firstValueFrom(this.http.delete(`/api/v1/owner/venues/${this.venueId()}/courts/${this.court().id}/pricing-rules/${rule.id}`));
      this.done.emit();
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  private problem(): string | null {
    if (!this.from() || !this.to()) {
      return 'Set the hours this price is for.';
    }
    if (this.from() === this.to() && this.to() !== '00:00') {
      return 'The hours start and end at the same time.';
    }
    const amount = Number(this.price());
    if (this.price() === '' || !Number.isFinite(amount) || amount < 0) {
      return `Enter the price for one booking, e.g. ${rupees(1200)}.`;
    }
    if (this.validFrom() && this.validTo() && this.validTo() < this.validFrom()) {
      return 'The "until" date is before the starting date.';
    }
    return null;
  }
}
