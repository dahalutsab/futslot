import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LoaderCircle, LucideAngularModule } from 'lucide-angular';

import { problemOf } from '../../core/api-error';
import { BlockReason, Slot } from '../../core/api.types';
import { localClock, rupees } from '../../shared/format';
import { Sheet } from '../../shared/sheet';
import { ScheduleApi } from './schedule-api';

type Mode = 'book' | 'block';

/** A free slot: book it for someone at the counter or on the phone, or block it. */
@Component({
  selector: 'fs-walk-in-panel',
  imports: [FormsModule, LucideAngularModule, Sheet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <fs-sheet [title]="courtName() + ', ' + timeRange()" [subtitle]="rupees(slot().price)" (closed)="closed.emit()">
      @if (canBook() && canBlock()) {
        <div class="mb-5 flex rounded-lg bg-surface-2 p-1" role="tablist">
          <button type="button" role="tab" [attr.aria-selected]="mode() === 'book'" (click)="mode.set('book')"
            class="h-10 flex-1 rounded-md font-medium text-muted" [class.bg-surface]="mode() === 'book'" [class.text-text]="mode() === 'book'">Book</button>
          <button type="button" role="tab" [attr.aria-selected]="mode() === 'block'" (click)="mode.set('block')"
            class="h-10 flex-1 rounded-md font-medium text-muted" [class.bg-surface]="mode() === 'block'" [class.text-text]="mode() === 'block'">Block</button>
        </div>
      }

      @if (mode() === 'book') {
        <form id="walk-in" class="space-y-4" (ngSubmit)="book()">
          <div>
            <label for="walk-in-phone" class="mb-1.5 block font-medium">Phone</label>
            <input id="walk-in-phone" name="phone" type="tel" inputmode="numeric" autocomplete="off" placeholder="98XXXXXXXX"
              class="tabular h-12 w-full rounded-lg border border-border bg-surface px-3 text-base"
              [ngModel]="phone()" (ngModelChange)="phone.set($event)" />
            <p class="mt-1.5 text-sm text-subtle">If they use FutSlot, the booking shows up in their app.</p>
          </div>
          <div>
            <label for="walk-in-name" class="mb-1.5 block font-medium">Name</label>
            <input id="walk-in-name" name="name" type="text" autocomplete="off"
              class="h-12 w-full rounded-lg border border-border bg-surface px-3 text-base"
              [ngModel]="name()" (ngModelChange)="name.set($event)" />
          </div>
          <fieldset>
            <legend class="mb-1.5 font-medium">How did they book?</legend>
            <div class="flex gap-2">
              @for (option of sources; track option.value) {
                <label class="flex h-11 flex-1 cursor-pointer items-center justify-center rounded-lg border font-medium"
                  [class]="source() === option.value ? 'border-ink bg-surface-2' : 'border-border text-muted'">
                  <input type="radio" name="source" class="sr-only" [value]="option.value"
                    [checked]="source() === option.value" (change)="source.set(option.value)" />
                  {{ option.label }}
                </label>
              }
            </div>
          </fieldset>
          <label class="flex cursor-pointer items-start gap-3 rounded-lg border border-border p-3">
            <input type="checkbox" class="mt-1 size-4 accent-current" [checked]="paidCash()" (change)="paidCash.set(!paidCash())" />
            <span>
              <span class="block font-medium">Paid {{ rupees(slot().price) }} in cash now</span>
              <span class="text-sm text-subtle">Leave unticked if they'll pay after the game.</span>
            </span>
          </label>
          <div>
            <label for="walk-in-notes" class="mb-1.5 block font-medium">Notes <span class="font-normal text-subtle">(optional)</span></label>
            <textarea id="walk-in-notes" name="notes" rows="2" maxlength="500"
              class="w-full rounded-lg border border-border bg-surface px-3 py-2 text-base"
              [ngModel]="notes()" (ngModelChange)="notes.set($event)"></textarea>
          </div>
        </form>
      } @else {
        <form id="walk-in" class="space-y-4" (ngSubmit)="block()">
          <div>
            <label for="block-reason" class="mb-1.5 block font-medium">Why?</label>
            <select id="block-reason" name="reason" class="h-12 w-full rounded-lg border border-border bg-surface px-3 text-base"
              [ngModel]="reason()" (ngModelChange)="reason.set($event)">
              @for (option of reasons; track option.value) {
                <option [value]="option.value">{{ option.label }}</option>
              }
            </select>
          </div>
          <div>
            <label for="block-note" class="mb-1.5 block font-medium">Note <span class="font-normal text-subtle">(optional)</span></label>
            <input id="block-note" name="note" type="text" maxlength="500" placeholder="e.g. Net repair"
              class="h-12 w-full rounded-lg border border-border bg-surface px-3 text-base"
              [ngModel]="note()" (ngModelChange)="note.set($event)" />
            <p class="mt-1.5 text-sm text-subtle">Players will see this slot as unavailable.</p>
          </div>
        </form>
      }

      @if (error(); as message) {
        <p class="mt-4 rounded-lg bg-danger-bg px-3 py-2 text-danger" role="alert">{{ message }}</p>
      }

      <div sheet-footer class="border-t border-border px-5 py-4 pb-[calc(1rem+env(safe-area-inset-bottom))]">
        <button type="submit" form="walk-in" [disabled]="busy()"
          class="flex h-12 w-full items-center justify-center gap-2 rounded-lg bg-ink font-medium text-ink-fg disabled:opacity-60">
          @if (busy()) {
            <lucide-icon [img]="spinner" [size]="18" class="animate-spin" />
          }
          {{ mode() === 'book' ? 'Book ' + timeRange() : 'Block ' + timeRange() }}
        </button>
      </div>
    </fs-sheet>
  `,
})
export class WalkInPanel implements OnInit {
  private readonly api = inject(ScheduleApi);

  readonly venueId = input.required<string>();

  readonly courtId = input.required<string>();

  readonly courtName = input.required<string>();

  readonly slot = input.required<Slot>();

  readonly canBook = input(true);

  readonly canBlock = input(false);

  readonly done = output<void>();

  readonly closed = output<void>();

  protected readonly rupees = rupees;

  protected readonly spinner = LoaderCircle;

  protected readonly mode = signal<Mode>('book');

  protected readonly phone = signal('');

  protected readonly name = signal('');

  protected readonly source = signal<'WALK_IN' | 'PHONE'>('WALK_IN');

  protected readonly paidCash = signal(false);

  protected readonly notes = signal('');

  protected readonly reason = signal<BlockReason>('MAINTENANCE');

  protected readonly note = signal('');

  protected readonly busy = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly sources = [
    { value: 'WALK_IN' as const, label: 'Walk-in' },
    { value: 'PHONE' as const, label: 'Phone call' },
  ];

  protected readonly reasons: { value: BlockReason; label: string }[] = [
    { value: 'MAINTENANCE', label: 'Maintenance' },
    { value: 'PRIVATE_EVENT', label: 'Private event' },
    { value: 'HOLIDAY', label: 'Closed (holiday)' },
    { value: 'OTHER', label: 'Other' },
  ];

  protected readonly timeRange = computed(() => `${localClock(this.slot().startsAt)}–${localClock(this.slot().endsAt)}`);

  ngOnInit(): void {
    if (!this.canBook()) {
      this.mode.set('block');
    }
  }

  protected async book(): Promise<void> {
    const digits = this.phone().replace(/\D/g, '');
    if (!/^(977)?\d{10}$/.test(digits)) {
      this.error.set('Enter their 10-digit mobile number.');
      return;
    }
    if (this.name().trim().length < 2) {
      this.error.set('Enter their name.');
      return;
    }
    await this.run(async () => {
      const booking = await this.api.book(this.venueId(), {
        courtId: this.courtId(),
        startsAt: this.slot().startsAt,
        customerName: this.name().trim(),
        customerPhone: digits,
        source: this.source(),
        notes: this.notes().trim() || undefined,
      });
      if (this.paidCash() && booking.totalAmount > 0) {
        await this.api.takePayment(this.venueId(), booking.id, booking.totalAmount, 'CASH');
      }
    });
  }

  protected async block(): Promise<void> {
    await this.run(() => this.api.block(this.venueId(), this.courtId(), this.slot().startsAt, this.slot().endsAt,
      this.reason(), this.note().trim() || undefined));
  }

  private async run(action: () => Promise<unknown>): Promise<void> {
    if (this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      await action();
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
