import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import { LoaderCircle, LucideAngularModule, Plus } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import { problemOf } from '../../core/api-error';
import { DiscountCode } from '../../core/api.types';
import { rupees, today } from '../../shared/format';
import { Sheet } from '../../shared/sheet';

const ZONE = 'Asia/Kathmandu';

/** YYYY-MM-DD of an instant in Nepal. */
function nepalDay(instant: string): string {
  return today(ZONE, new Date(instant));
}

function shortDate(day: string): string {
  const [y, m, d] = day.split('-').map(Number);
  return new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', timeZone: 'UTC' }).format(new Date(Date.UTC(y, m - 1, d)));
}

/** "20% off, up to Rs 300" or "Rs 200 off". */
export function discountLabel(code: Pick<DiscountCode, 'type' | 'value' | 'maxDiscountAmount' | 'minBookingAmount'>): string {
  const off = code.type === 'PERCENT' ? `${code.value}% off` : `${rupees(code.value)} off`;
  const cap = code.type === 'PERCENT' && code.maxDiscountAmount ? `, up to ${rupees(code.maxDiscountAmount)}` : '';
  const min = code.minBookingAmount ? ` on bookings of ${rupees(code.minBookingAmount)} or more` : '';
  return off + cap + min;
}

interface Editing {
  code: DiscountCode | null;
  text: string;
  description: string;
  type: 'PERCENT' | 'FIXED';
  value: string;
  max: string;
  min: string;
  from: string;
  to: string;
  usageLimit: string;
  perUserLimit: string;
  active: boolean;
}

/** Codes players type when booking for money off, e.g. DASHAIN20. */
@Component({
  selector: 'fs-discount-codes',
  imports: [LucideAngularModule, Sheet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-wrap items-center justify-between gap-3">
      <p class="text-muted">Share a code on social media or with a team; players enter it when they book.</p>
      <button type="button" (click)="add()" class="flex h-11 items-center gap-2 rounded-md bg-ink px-4 font-medium text-ink-fg hover:opacity-90">
        <lucide-icon [img]="icons.Plus" [size]="18" />New code
      </button>
    </div>
    @if (error(); as message) {
      <p class="mt-4 rounded-md border border-border bg-surface px-6 py-8 text-center text-muted">{{ message }}</p>
    } @else if (codes(); as list) {
      @if (list.length === 0) {
        <p class="mt-4 rounded-md border border-dashed border-border bg-surface px-6 py-8 text-center text-muted">No codes yet. Try one for a festival or a quiet weekday morning.</p>
      } @else {
        <ul class="mt-4 divide-y divide-border overflow-hidden rounded-md border border-border bg-surface">
          @for (code of list; track code.id) {
            <li>
              <button type="button" (click)="edit(code)" class="flex w-full flex-wrap items-center gap-x-4 gap-y-1 px-4 py-3 text-left hover:bg-surface-2">
                <span class="tabular min-w-32 font-semibold tracking-wide" [class.text-muted]="!live(code)">{{ code.code }}</span>
                <span class="min-w-0 flex-1">
                  <span class="block" [class.text-muted]="!live(code)">{{ label(code) }}</span>
                  @if (code.description) {
                    <span class="block truncate text-sm text-subtle">{{ code.description }}</span>
                  }
                </span>
                <span class="text-right text-sm">
                  <span class="tabular block">{{ code.usedCount }}{{ code.usageLimit ? ' of ' + code.usageLimit : '' }} used</span>
                  <span class="block text-muted">{{ state(code) }}</span>
                </span>
              </button>
            </li>
          }
        </ul>
      }
    } @else {
      <div class="mt-4 h-32 animate-pulse rounded-md bg-surface" aria-label="Loading"></div>
    }

    @if (editing(); as e) {
      <fs-sheet [title]="e.code ? e.code.code : 'New code'" [subtitle]="e.code ? e.code.usedCount + ' used so far' : null" (closed)="editing.set(null)">
        <form id="code-form" class="space-y-4" (submit)="$event.preventDefault(); save()">
          @if (!e.code) {
            <div>
              <label for="code-text" class="mb-1.5 block font-medium">Code</label>
              <input id="code-text" type="text" maxlength="32" autocomplete="off" autocapitalize="characters" [value]="e.text"
                (input)="patch({ text: $any($event.target).value.toUpperCase() })" placeholder="e.g. DASHAIN20"
                class="tabular h-12 w-full rounded-md border border-border bg-surface px-3 text-base uppercase tracking-wide placeholder:normal-case placeholder:tracking-normal" />
              <p class="mt-1.5 text-sm text-subtle">3 to 32 letters or digits. It can't be changed later.</p>
            </div>
            <fieldset>
              <legend class="mb-1.5 font-medium">Discount</legend>
              <div class="flex gap-2">
                <label class="flex h-11 flex-1 cursor-pointer items-center justify-center rounded-md border font-medium" [class]="e.type === 'PERCENT' ? 'border-ink bg-surface-2' : 'border-border text-muted'">
                  <input type="radio" name="type" class="sr-only" [checked]="e.type === 'PERCENT'" (change)="patch({ type: 'PERCENT' })" />Percent off
                </label>
                <label class="flex h-11 flex-1 cursor-pointer items-center justify-center rounded-md border font-medium" [class]="e.type === 'FIXED' ? 'border-ink bg-surface-2' : 'border-border text-muted'">
                  <input type="radio" name="type" class="sr-only" [checked]="e.type === 'FIXED'" (change)="patch({ type: 'FIXED' })" />Rupees off
                </label>
              </div>
            </fieldset>
          }
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label for="code-value" class="mb-1.5 block font-medium">{{ e.type === 'PERCENT' ? 'Percent off' : 'Rupees off' }}</label>
              <input id="code-value" type="number" inputmode="numeric" min="1" [max]="e.type === 'PERCENT' ? 100 : null" [value]="e.value" (input)="patch({ value: $any($event.target).value })"
                class="tabular h-12 w-full rounded-md border border-border bg-surface px-3 text-base" />
            </div>
            @if (e.type === 'PERCENT') {
              <div>
                <label for="code-max" class="mb-1.5 block font-medium">Up to Rs <span class="font-normal text-subtle">(optional)</span></label>
                <input id="code-max" type="number" inputmode="numeric" min="1" [value]="e.max" (input)="patch({ max: $any($event.target).value })"
                  class="tabular h-12 w-full rounded-md border border-border bg-surface px-3 text-base" />
              </div>
            }
          </div>
          <div>
            <label for="code-description" class="mb-1.5 block font-medium">Note <span class="font-normal text-subtle">(optional, players see it)</span></label>
            <input id="code-description" type="text" maxlength="200" [value]="e.description" (input)="patch({ description: $any($event.target).value })"
              placeholder="e.g. Dashain offer" class="h-12 w-full rounded-md border border-border bg-surface px-3 text-base" />
          </div>
          <details class="rounded-md border border-border" [open]="!!(e.min || e.from || e.to || e.usageLimit || e.perUserLimit)">
            <summary class="flex h-11 cursor-pointer items-center px-3 font-medium">Limits</summary>
            <div class="space-y-4 border-t border-border p-3">
              <div class="grid grid-cols-2 gap-3">
                <div>
                  <label for="code-from" class="mb-1.5 block text-sm font-medium">From</label>
                  <input id="code-from" type="date" [value]="e.from" (input)="patch({ from: $any($event.target).value })" class="h-11 w-full rounded-md border border-border bg-surface px-2 text-base" />
                </div>
                <div>
                  <label for="code-to" class="mb-1.5 block text-sm font-medium">Until</label>
                  <input id="code-to" type="date" [value]="e.to" (input)="patch({ to: $any($event.target).value })" class="h-11 w-full rounded-md border border-border bg-surface px-2 text-base" />
                </div>
              </div>
              <div class="grid grid-cols-2 gap-3">
                <div>
                  <label for="code-uses" class="mb-1.5 block text-sm font-medium">Total uses</label>
                  <input id="code-uses" type="number" inputmode="numeric" min="1" placeholder="No limit" [value]="e.usageLimit" (input)="patch({ usageLimit: $any($event.target).value })"
                    class="tabular h-11 w-full rounded-md border border-border bg-surface px-3 text-base" />
                </div>
                <div>
                  <label for="code-per" class="mb-1.5 block text-sm font-medium">Uses per player</label>
                  <input id="code-per" type="number" inputmode="numeric" min="1" placeholder="No limit" [value]="e.perUserLimit" (input)="patch({ perUserLimit: $any($event.target).value })"
                    class="tabular h-11 w-full rounded-md border border-border bg-surface px-3 text-base" />
                </div>
              </div>
              <div>
                <label for="code-min" class="mb-1.5 block text-sm font-medium">Only on bookings of at least Rs</label>
                <input id="code-min" type="number" inputmode="numeric" min="0" [value]="e.min" (input)="patch({ min: $any($event.target).value })"
                  class="tabular h-11 w-40 rounded-md border border-border bg-surface px-3 text-base" />
              </div>
            </div>
          </details>
          @if (e.code) {
            <label class="flex cursor-pointer items-center gap-3 rounded-md border border-border p-3">
              <input type="checkbox" class="size-4 accent-current" [checked]="e.active" (change)="patch({ active: !e.active })" />
              <span class="font-medium">Players can use it</span>
            </label>
          }
        </form>
        @if (formError(); as message) {
          <p class="mt-4 rounded-md bg-danger-bg px-3 py-2 text-danger" role="alert">{{ message }}</p>
        }
        <div sheet-footer class="border-t border-border px-5 py-4 pb-[calc(1rem+env(safe-area-inset-bottom))]">
          <button type="submit" form="code-form" [disabled]="saving()" class="flex h-12 w-full items-center justify-center gap-2 rounded-md bg-ink font-medium text-ink-fg disabled:opacity-60">
            @if (saving()) {
              <lucide-icon [img]="icons.LoaderCircle" [size]="18" class="animate-spin" />
            }
            {{ e.code ? 'Save' : 'Create code' }}
          </button>
        </div>
      </fs-sheet>
    }
  `,
})
export class DiscountCodes implements OnInit {
  private readonly http = inject(HttpClient);

  readonly venueId = input.required<string>();

  protected readonly icons = { LoaderCircle, Plus };

  protected readonly label = discountLabel;

  protected readonly codes = signal<DiscountCode[] | null>(null);

  protected readonly error = signal<string | null>(null);

  protected readonly editing = signal<Editing | null>(null);

  protected readonly saving = signal(false);

  protected readonly formError = signal<string | null>(null);

  ngOnInit(): void {
    void this.load();
  }

  /** Usable today. */
  protected live(code: DiscountCode): boolean {
    const now = new Date().toISOString();
    return code.active && (!code.validFrom || code.validFrom <= now) && (!code.validTo || code.validTo >= now)
      && (!code.usageLimit || code.usedCount < code.usageLimit);
  }

  protected state(code: DiscountCode): string {
    const now = new Date().toISOString();
    if (!code.active) {
      return 'Turned off';
    }
    if (code.usageLimit && code.usedCount >= code.usageLimit) {
      return 'Used up';
    }
    if (code.validFrom && code.validFrom > now) {
      return `Starts ${shortDate(nepalDay(code.validFrom))}`;
    }
    if (code.validTo) {
      return code.validTo < now ? 'Ended' : `Until ${shortDate(nepalDay(code.validTo))}`;
    }
    return 'Running';
  }

  protected add(): void {
    this.formError.set(null);
    this.editing.set({ code: null, text: '', description: '', type: 'PERCENT', value: '10', max: '', min: '', from: '', to: '',
      usageLimit: '', perUserLimit: '1', active: true });
  }

  protected edit(code: DiscountCode): void {
    this.formError.set(null);
    this.editing.set({ code, text: code.code, description: code.description ?? '', type: code.type, value: String(code.value),
      max: code.maxDiscountAmount?.toString() ?? '', min: code.minBookingAmount?.toString() ?? '',
      from: code.validFrom ? nepalDay(code.validFrom) : '', to: code.validTo ? nepalDay(code.validTo) : '',
      usageLimit: code.usageLimit?.toString() ?? '', perUserLimit: code.perUserLimit?.toString() ?? '', active: code.active });
  }

  protected patch(change: Partial<Editing>): void {
    this.editing.update((e) => (e ? { ...e, ...change } : e));
  }

  protected async save(): Promise<void> {
    const e = this.editing();
    if (!e || this.saving()) {
      return;
    }
    const value = Number(e.value);
    if (!/^[A-Z0-9_-]{3,32}$/.test(e.text.trim())) {
      this.formError.set('Use 3 to 32 letters, digits, - or _ for the code.');
      return;
    }
    if (!(value > 0) || (e.type === 'PERCENT' && value > 100)) {
      this.formError.set(e.type === 'PERCENT' ? 'Set a discount between 1% and 100%.' : 'Set how many rupees off.');
      return;
    }
    if (e.from && e.to && e.to < e.from) {
      this.formError.set('The end date is before the start date.');
      return;
    }
    const number = (text: string) => (text.trim() ? Number(text) : null);
    const body = {
      code: e.text.trim(),
      description: e.description.trim() || null,
      type: e.type,
      value,
      maxDiscountAmount: e.type === 'PERCENT' ? number(e.max) : null,
      minBookingAmount: number(e.min),
      // Whole days in Nepal: from the start of the first to the end of the last
      validFrom: e.from ? `${e.from}T00:00:00+05:45` : null,
      validTo: e.to ? `${e.to}T23:59:59+05:45` : null,
      usageLimit: number(e.usageLimit),
      perUserLimit: number(e.perUserLimit),
      active: e.active,
    };
    this.saving.set(true);
    this.formError.set(null);
    const base = `/api/v1/owner/venues/${this.venueId()}/discount-codes`;
    try {
      const saved = e.code
        ? await firstValueFrom(this.http.put<DiscountCode>(`${base}/${e.code.id}`, body))
        : await firstValueFrom(this.http.post<DiscountCode>(base, body));
      this.codes.update((list) => [saved, ...(list ?? []).filter((c) => c.id !== saved.id)]);
      this.editing.set(null);
    }
    catch (error) {
      this.formError.set(problemOf(error).message);
    }
    finally {
      this.saving.set(false);
    }
  }

  private async load(): Promise<void> {
    try {
      this.codes.set(await firstValueFrom(this.http.get<DiscountCode[]>(`/api/v1/owner/venues/${this.venueId()}/discount-codes`)));
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
