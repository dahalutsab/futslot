import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, input, OnInit, output, signal } from '@angular/core';
import { LoaderCircle, LucideAngularModule } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import { problemOf } from '../../core/api-error';
import { Court, CourtFormat, CourtSurface } from '../../core/api.types';
import { Sheet } from '../../shared/sheet';

export const FORMATS: { value: CourtFormat; label: string }[] = [
  { value: 'FIVE_A_SIDE', label: '5-a-side' },
  { value: 'SIX_A_SIDE', label: '6-a-side' },
  { value: 'SEVEN_A_SIDE', label: '7-a-side' },
];

export const SURFACES: { value: CourtSurface; label: string }[] = [
  { value: 'ARTIFICIAL_TURF', label: 'Artificial turf' },
  { value: 'HARDWOOD', label: 'Hardwood' },
  { value: 'CONCRETE', label: 'Concrete' },
  { value: 'RUBBER', label: 'Rubber' },
];

const LENGTHS = [30, 45, 60, 90, 120];

/** Adds a court (optionally with another court's prices) or changes one. */
@Component({
  selector: 'fs-court-panel',
  imports: [LucideAngularModule, Sheet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <fs-sheet [title]="court() ? 'Edit ' + court()!.name : 'Add a court'" (closed)="closed.emit()">
      <form id="court-form" class="space-y-4" (submit)="$event.preventDefault(); save()">
        <div>
          <label for="court-name" class="mb-1.5 block font-medium">Name</label>
          <input id="court-name" type="text" maxlength="60" autocomplete="off" placeholder="e.g. Court A" [value]="name()"
            (input)="name.set($any($event.target).value)" class="h-12 w-full rounded-lg border border-border bg-surface px-3 text-base" />
        </div>
        <fieldset>
          <legend class="mb-1.5 font-medium">Size</legend>
          <div class="flex gap-2">
            @for (option of formats; track option.value) {
              <label class="flex h-11 flex-1 cursor-pointer items-center justify-center rounded-lg border font-medium"
                [class]="format() === option.value ? 'border-ink bg-surface-2' : 'border-border text-muted'">
                <input type="radio" name="format" class="sr-only" [checked]="format() === option.value" (change)="format.set(option.value)" />
                {{ option.label }}
              </label>
            }
          </div>
        </fieldset>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label for="court-surface" class="mb-1.5 block font-medium">Surface</label>
            <select id="court-surface" (change)="surface.set($any($event.target).value)"
              class="h-12 w-full rounded-lg border border-border bg-surface px-3 text-base">
              @for (option of surfaces; track option.value) {
                <option [value]="option.value" [selected]="surface() === option.value">{{ option.label }}</option>
              }
            </select>
          </div>
          <div>
            <label for="court-length" class="mb-1.5 block font-medium">Each booking</label>
            <select id="court-length" (change)="length.set(+$any($event.target).value)"
              class="h-12 w-full rounded-lg border border-border bg-surface px-3 text-base">
              @for (minutes of lengths(); track minutes) {
                <option [value]="minutes" [selected]="length() === minutes">{{ minutes < 60 ? minutes + ' min' : minutes / 60 + (minutes === 60 ? ' hour' : ' hours') }}</option>
              }
            </select>
          </div>
        </div>
        @if (court() && length() !== court()!.slotDurationMinutes) {
          <p class="text-sm text-muted">Bookings already made keep their length. New ones use the new length.</p>
        }
        <label class="flex cursor-pointer items-center gap-3 rounded-lg border border-border p-3">
          <input type="checkbox" class="size-4 accent-current" [checked]="indoor()" (change)="indoor.set(!indoor())" />
          <span class="font-medium">Indoor <span class="font-normal text-subtle">(covered from rain)</span></span>
        </label>
        @if (court()) {
          <label class="flex cursor-pointer items-start gap-3 rounded-lg border border-border p-3">
            <input type="checkbox" class="mt-1 size-4 accent-current" [checked]="active()" (change)="active.set(!active())" />
            <span>
              <span class="block font-medium">Players can book this court</span>
              <span class="text-sm text-subtle">Untick while it's closed for repairs; it disappears from the app and the schedule until you tick it again.</span>
            </span>
          </label>
        } @else if (others().length > 0) {
          <div>
            <label for="court-copy" class="mb-1.5 block font-medium">Prices</label>
            <select id="court-copy" (change)="copyFrom.set($any($event.target).value || null)"
              class="h-12 w-full rounded-lg border border-border bg-surface px-3 text-base">
              @for (other of others(); track other.id) {
                <option [value]="other.id" [selected]="copyFrom() === other.id">Same as {{ other.name }}</option>
              }
              <option value="" [selected]="!copyFrom()">I'll set them myself</option>
            </select>
          </div>
        }
      </form>

      @if (error(); as message) {
        <p class="mt-4 rounded-lg bg-danger-bg px-3 py-2 text-danger" role="alert">{{ message }}</p>
      }

      <div sheet-footer class="border-t border-border px-5 py-4 pb-[calc(1rem+env(safe-area-inset-bottom))]">
        <button type="submit" form="court-form" [disabled]="busy()"
          class="flex h-12 w-full items-center justify-center gap-2 rounded-lg bg-ink font-medium text-ink-fg disabled:opacity-60">
          @if (busy()) {
            <lucide-icon [img]="spinner" [size]="18" class="animate-spin" />
          }
          {{ court() ? 'Save' : 'Add court' }}
        </button>
      </div>
    </fs-sheet>
  `,
})
export class CourtPanel implements OnInit {
  private readonly http = inject(HttpClient);

  readonly venueId = input.required<string>();

  /** The court to edit, or null to add one. */
  readonly court = input<Court | null>(null);

  /** Courts whose prices a new court can start from. */
  readonly others = input<Court[]>([]);

  readonly done = output<void>();

  readonly closed = output<void>();

  protected readonly formats = FORMATS;

  protected readonly surfaces = SURFACES;

  protected readonly spinner = LoaderCircle;

  protected readonly name = signal('');

  protected readonly format = signal<CourtFormat>('FIVE_A_SIDE');

  protected readonly surface = signal<CourtSurface>('ARTIFICIAL_TURF');

  protected readonly length = signal(60);

  protected readonly indoor = signal(false);

  protected readonly active = signal(true);

  protected readonly copyFrom = signal<string | null>(null);

  protected readonly busy = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly lengths = signal(LENGTHS);

  ngOnInit(): void {
    const court = this.court();
    if (court) {
      this.name.set(court.name);
      this.format.set(court.format);
      this.surface.set(court.surface);
      this.length.set(court.slotDurationMinutes);
      this.indoor.set(court.indoor);
      this.active.set(court.status === 'ACTIVE');
      if (!LENGTHS.includes(court.slotDurationMinutes)) {
        this.lengths.set([...LENGTHS, court.slotDurationMinutes].sort((a, b) => a - b));
      }
    }
    else {
      const [first] = this.others();
      this.name.set(`Court ${String.fromCharCode(65 + this.others().length)}`);
      if (first) {
        this.format.set(first.format);
        this.surface.set(first.surface);
        this.length.set(first.slotDurationMinutes);
        this.indoor.set(first.indoor);
        this.copyFrom.set(first.id);
      }
    }
  }

  protected async save(): Promise<void> {
    if (this.busy()) {
      return;
    }
    if (!this.name().trim()) {
      this.error.set('Give the court a name players will recognise, like "Court A".');
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    const base = `/api/v1/owner/venues/${this.venueId()}/courts`;
    const body = {
      name: this.name().trim(),
      format: this.format(),
      surface: this.surface(),
      indoor: this.indoor(),
      slotDurationMinutes: this.length(),
      active: this.active(),
    };
    try {
      const existing = this.court();
      if (existing) {
        await firstValueFrom(this.http.put<Court>(`${base}/${existing.id}`, { ...body, sortOrder: existing.sortOrder }));
      }
      else {
        const created = await firstValueFrom(this.http.post<Court>(base, body));
        const source = this.others().find((c) => c.id === this.copyFrom());
        for (const rule of source?.pricingRules ?? []) {
          const { id: _, ...copy } = rule;
          await firstValueFrom(this.http.post(`${base}/${created.id}/pricing-rules`, copy));
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
}
