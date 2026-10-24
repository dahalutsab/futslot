import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CircleCheck, LucideAngularModule, Wallet } from 'lucide-angular';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { Payment } from '../../core/api.types';
import { Queue } from '../../core/queue';
import { ago, methodLabel, rupees } from '../../shared/format';
import { paymentFor } from '../../shared/labels';
import { NoteForm } from '../../shared/note-form';

/**
 * Money people say they sent FutSlot's own wallet or account: players topping up their booking deposit, venues
 * paying invoices and featured listings. Check each arrived, then approve it; if it didn't, say why.
 */
@Component({
  selector: 'fs-payments',
  imports: [LucideAngularModule, NoteForm],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-4xl px-4 py-6 lg:px-8 lg:py-8">
      <h1 class="text-3xl lg:text-4xl">Payments to check</h1>
      <p class="mt-1 text-muted">Look for each one in FutSlot's wallet or account, then approve it. A deposit top-up approved here fills the player's deposit straight away.</p>
      @if (error(); as message) {
        <p class="mt-4 text-danger" role="alert">{{ message }}</p>
      } @else if (payments(); as list) {
        @if (list.length === 0) {
          <div class="mt-6 flex flex-col items-center rounded-md border border-dashed border-border bg-surface px-6 py-14 text-center">
            <lucide-icon [img]="icons.CircleCheck" [size]="28" class="text-confirmed-line" />
            <p class="mt-3 font-medium">All caught up</p>
          </div>
        } @else {
          <ul class="mt-4 space-y-3">
            @for (p of list; track p.id) {
              <li class="rounded-md border border-border bg-surface p-4">
                <div class="flex flex-wrap items-start gap-x-4 gap-y-1">
                  <div class="min-w-0 flex-1">
                    <p class="font-medium">{{ p.customerName }}</p>
                    <p class="text-sm text-muted">{{ purpose(p) }} · {{ fmt.ago(p.createdAt) }}</p>
                  </div>
                  <div class="text-right">
                    <p class="tabular text-lg font-semibold">{{ fmt.rupees(p.amount) }}</p>
                    <p class="text-sm text-muted">{{ fmt.methodLabel(p.method) }}</p>
                  </div>
                </div>
                <p class="mt-3 flex items-center gap-2 rounded-md bg-verify-bg px-3 py-2 text-verify-fg">
                  <lucide-icon [img]="icons.Wallet" [size]="16" />
                  <span class="text-sm">Transaction</span>
                  <span class="tabular select-all font-medium">{{ p.transactionId }}</span>
                </p>
                @if (rowError()?.id === p.id) {
                  <p class="mt-2 text-danger" role="alert">{{ rowError()?.message }}</p>
                }
                <div class="mt-3">
                  @if (rejecting() === p.id) {
                    <fs-note-form [id]="'reason-' + p.id" label="What's wrong? They're told." action="Reject payment" [danger]="true" [busy]="busy() === p.id"
                      placeholder="e.g. Not in our eSewa" (done)="reject(p, $event)" (cancelled)="rejecting.set(null)" />
                  } @else {
                    <div class="flex gap-2">
                      <button type="button" (click)="approve(p)" [disabled]="busy() === p.id" class="h-11 rounded-md bg-ink px-5 font-medium text-ink-fg disabled:opacity-60">Approve</button>
                      <button type="button" (click)="rejecting.set(p.id)" class="h-11 rounded-md border border-border px-4 font-medium hover:bg-surface-2">Not received</button>
                    </div>
                  }
                </div>
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
export class Payments {
  private readonly api = inject(Api);

  private readonly queue = inject(Queue);

  protected readonly icons = { CircleCheck, Wallet };

  protected readonly fmt = { ago, methodLabel, rupees };

  protected readonly purpose = paymentFor;

  protected readonly payments = signal<Payment[] | null>(null);

  protected readonly rejecting = signal<string | null>(null);

  protected readonly busy = signal<string | null>(null);

  protected readonly rowError = signal<{ id: string; message: string } | null>(null);

  protected readonly error = signal<string | null>(null);

  constructor() {
    void this.load();
  }

  protected approve(payment: Payment): Promise<void> {
    return this.act(payment, () => this.api.approvePayment(payment.id));
  }

  protected reject(payment: Payment, reason: string): Promise<void> {
    return this.act(payment, () => this.api.rejectPayment(payment.id, reason));
  }

  private async act(payment: Payment, action: () => Promise<unknown>): Promise<void> {
    if (this.busy()) {
      return;
    }
    this.busy.set(payment.id);
    this.rowError.set(null);
    try {
      await action();
      this.rejecting.set(null);
      this.payments.update((list) => list?.filter((p) => p.id !== payment.id) ?? null);
      void this.queue.refresh();
    }
    catch (error) {
      this.rowError.set({ id: payment.id, message: problemOf(error).message });
    }
    finally {
      this.busy.set(null);
    }
  }

  private async load(): Promise<void> {
    try {
      this.payments.set(await this.api.pendingPayments());
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
