import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CircleCheck, LucideAngularModule } from 'lucide-angular';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { PaymentMethod, Payout } from '../../core/api.types';
import { Queue } from '../../core/queue';
import { methodLabel, monthLabel, rupees } from '../../shared/format';

const SEND_METHODS: PaymentMethod[] = ['BANK_TRANSFER', 'ESEWA', 'KHALTI'];

/**
 * Late-cancellation and no-show fees taken from players' deposits, owed to venues. After each month ends they
 * become one payout per venue; send it to the venue's own account and record the reference.
 */
@Component({
  selector: 'fs-payouts',
  imports: [FormsModule, RouterLink, LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-4xl px-4 py-6 lg:px-8 lg:py-8">
      <h1 class="text-3xl lg:text-4xl">Venue payouts</h1>
      <p class="mt-1 text-muted">Deposit fees FutSlot collected for venues, one payout per venue and month. FutSlot keeps none of it.</p>
      @if (error(); as message) {
        <p class="mt-4 text-danger" role="alert">{{ message }}</p>
      } @else if (payouts(); as list) {
        @if (list.length === 0) {
          <div class="mt-6 flex flex-col items-center rounded-md border border-dashed border-border bg-surface px-6 py-14 text-center">
            <lucide-icon [img]="icons.CircleCheck" [size]="28" class="text-confirmed-line" />
            <p class="mt-3 font-medium">Nothing to pay out</p>
            <p class="mt-1 max-w-sm text-muted">After a month ends, its deposit fees show up here the next morning.</p>
          </div>
        } @else {
          <ul class="mt-4 space-y-3">
            @for (p of list; track p.id) {
              <li class="rounded-md border border-border bg-surface p-4">
                <div class="flex flex-wrap items-start gap-x-4 gap-y-1">
                  <div class="min-w-0 flex-1">
                    <a [routerLink]="['/venues', p.venueId]" class="font-medium hover:underline">{{ p.venueName }}</a>
                    <p class="text-sm text-muted">Fees for {{ fmt.monthLabel(p.periodStart) }}</p>
                  </div>
                  <p class="tabular text-lg font-semibold">{{ fmt.rupees(p.amount) }}</p>
                </div>
                <p class="mt-3 rounded-md bg-surface-2 px-3 py-2">
                  <span class="text-sm text-muted">Send to the venue's</span>
                  <span class="block whitespace-pre-line font-medium">{{ p.paymentInstructions ?? 'No payment details set: ask the venue' }}</span>
                </p>
                @if (rowError()?.id === p.id) {
                  <p class="mt-2 text-danger" role="alert">{{ rowError()?.message }}</p>
                }
                @if (sending() === p.id) {
                  <form class="mt-3 space-y-3" (ngSubmit)="paid(p)">
                    <fieldset>
                      <legend class="mb-1.5 font-medium">Sent by</legend>
                      <div class="flex flex-wrap gap-2">
                        @for (m of methods; track m) {
                          <button type="button" (click)="method.set(m)" [attr.aria-pressed]="method() === m" class="h-10 rounded-md border px-4 text-sm font-medium"
                            [class]="method() === m ? 'border-ink bg-surface-2' : 'border-border text-muted'">{{ fmt.methodLabel(m) }}</button>
                        }
                      </div>
                    </fieldset>
                    <div>
                      <label [for]="'payout-ref-' + p.id" class="mb-1.5 block font-medium">Transfer reference</label>
                      <input [id]="'payout-ref-' + p.id" name="reference" type="text" maxlength="100" class="tabular h-11 w-full rounded-md border border-border bg-surface px-3 text-base"
                        [ngModel]="reference()" (ngModelChange)="reference.set($event)" />
                    </div>
                    <div class="flex gap-2">
                      <button type="submit" [disabled]="busy() === p.id" class="h-11 rounded-md bg-ink px-5 font-medium text-ink-fg disabled:opacity-60">Mark {{ fmt.rupees(p.amount) }} sent</button>
                      <button type="button" (click)="sending.set(null)" class="h-11 rounded-md px-4 font-medium">Back</button>
                    </div>
                    <p class="text-sm text-subtle">The venue's owner is told it's on its way.</p>
                  </form>
                } @else {
                  <button type="button" (click)="start(p)" class="mt-3 h-11 rounded-md bg-ink px-5 font-medium text-ink-fg">I've sent it</button>
                }
              </li>
            }
          </ul>
        }
      } @else {
        <div class="mt-6 h-40 animate-pulse rounded-md bg-surface" aria-label="Loading"></div>
      }
    </div>
  `,
})
export class Payouts {
  private readonly api = inject(Api);

  private readonly queue = inject(Queue);

  protected readonly icons = { CircleCheck };

  protected readonly fmt = { methodLabel, monthLabel, rupees };

  protected readonly methods = SEND_METHODS;

  protected readonly payouts = signal<Payout[] | null>(null);

  protected readonly sending = signal<string | null>(null);

  protected readonly method = signal<PaymentMethod>('BANK_TRANSFER');

  protected readonly reference = signal('');

  protected readonly busy = signal<string | null>(null);

  protected readonly rowError = signal<{ id: string; message: string } | null>(null);

  protected readonly error = signal<string | null>(null);

  constructor() {
    this.api.payouts().then((list) => this.payouts.set(list), (error) => this.error.set(problemOf(error).message));
  }

  protected start(payout: Payout): void {
    this.sending.set(payout.id);
    this.reference.set('');
    this.rowError.set(null);
  }

  protected async paid(payout: Payout): Promise<void> {
    const reference = this.reference().trim();
    if (reference.length < 3) {
      this.rowError.set({ id: payout.id, message: 'Enter the transfer reference.' });
      return;
    }
    if (this.busy()) {
      return;
    }
    this.busy.set(payout.id);
    this.rowError.set(null);
    try {
      await this.api.payoutPaid(payout.id, this.method(), reference);
      this.sending.set(null);
      this.payouts.update((list) => list?.filter((p) => p.id !== payout.id) ?? null);
      void this.queue.refresh();
    }
    catch (error) {
      this.rowError.set({ id: payout.id, message: problemOf(error).message });
    }
    finally {
      this.busy.set(null);
    }
  }
}
