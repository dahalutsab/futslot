import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { CircleCheck, LucideAngularModule } from 'lucide-angular';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { Withdrawal } from '../../core/api.types';
import { Queue } from '../../core/queue';
import { ago, clockAt, dayLabel, methodLabel, phone, rupees, today } from '../../shared/format';
import { NoteForm } from '../../shared/note-form';

type Tab = 'due' | 'waiting' | 'sent';

const NEPAL = 'Asia/Kathmandu';

/**
 * Players taking their booking deposit back. FutSlot sends each one by hand once its 3-day wait is over (so a
 * late fee can still come out), then records the transfer's reference. The player is told.
 */
@Component({
  selector: 'fs-withdrawals',
  imports: [LucideAngularModule, NoteForm],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mx-auto max-w-4xl px-4 py-6 lg:px-8 lg:py-8">
      <h1 class="text-2xl font-semibold tracking-tight">Deposits to send</h1>
      <p class="mt-1 text-muted">Players taking their booking deposit back. Send each from FutSlot's account once it's due, then record the reference.</p>
      <div class="mt-4 flex gap-1 rounded-lg bg-surface-2 p-1" role="tablist" aria-label="Withdrawals">
        @for (t of tabs; track t.value) {
          <button type="button" role="tab" [attr.aria-selected]="tab() === t.value" (click)="tab.set(t.value)"
            class="h-10 flex-1 rounded-md font-medium text-muted" [class.bg-surface]="tab() === t.value" [class.text-text]="tab() === t.value">
            {{ t.label }}@if (t.value !== 'sent' && count(t.value)) { <span class="tabular"> · {{ count(t.value) }}</span> }
          </button>
        }
      </div>

      @if (error(); as message) {
        <p class="mt-4 text-danger" role="alert">{{ message }}</p>
      } @else if (shown(); as list) {
        @if (list.length === 0) {
          <div class="mt-6 flex flex-col items-center rounded-xl border border-dashed border-border bg-surface px-6 py-14 text-center">
            <lucide-icon [img]="icons.CircleCheck" [size]="28" class="text-confirmed-line" />
            <p class="mt-3 font-medium">{{ tab() === 'due' ? 'Nothing to send now' : tab() === 'waiting' ? 'Nothing waiting' : 'Nothing sent yet' }}</p>
          </div>
        } @else {
          <ul class="mt-4 space-y-3">
            @for (w of list; track w.id) {
              <li class="rounded-xl border border-border bg-surface p-4">
                <div class="flex flex-wrap items-start gap-x-4 gap-y-1">
                  <div class="min-w-0 flex-1">
                    <p class="font-medium">{{ w.playerName }}</p>
                    @if (w.playerPhone) {
                      <a [href]="'tel:' + w.playerPhone" class="tabular text-sm text-muted hover:underline">{{ fmt.phone(w.playerPhone) }}</a>
                    }
                  </div>
                  <p class="tabular text-lg font-semibold">{{ fmt.rupees(w.amount) }}</p>
                </div>
                <p class="mt-3 rounded-lg bg-surface-2 px-3 py-2">
                  <span class="text-sm text-muted">Send to their {{ fmt.methodLabel(w.method) }}</span>
                  <span class="tabular block select-all font-medium">{{ w.sendTo }}</span>
                </p>
                @if (w.status === 'PAID') {
                  <p class="mt-2 text-sm text-muted">Sent {{ w.paidAt ? fmt.ago(w.paidAt) : '' }}@if (w.reference) { · <span class="tabular">{{ w.reference }}</span> }</p>
                } @else if (!isDue(w)) {
                  <p class="mt-2 text-sm text-muted">Asked {{ fmt.ago(w.createdAt) }}. Due {{ fmt.dayLabel(localDay(w.dueAt)) }}, {{ fmt.clockAt(w.dueAt, zone) }}.</p>
                } @else {
                  @if (rowError()?.id === w.id) {
                    <p class="mt-2 text-danger" role="alert">{{ rowError()?.message }}</p>
                  }
                  <div class="mt-3">
                    @if (sending() === w.id) {
                      <fs-note-form [id]="'ref-' + w.id" label="Transfer reference" [action]="'Mark ' + fmt.rupees(w.amount) + ' sent'" [maxLength]="100"
                        [busy]="busy() === w.id" placeholder="Transaction ID from the transfer" (done)="paid(w, $event)" (cancelled)="sending.set(null)" />
                    } @else {
                      <button type="button" (click)="sending.set(w.id)" class="h-11 rounded-lg bg-ink px-5 font-medium text-ink-fg">I've sent it</button>
                    }
                  </div>
                }
              </li>
            }
          </ul>
        }
      } @else {
        <div class="mt-6 h-40 animate-pulse rounded-xl bg-surface" aria-label="Loading"></div>
      }
    </div>
  `,
})
export class Withdrawals {
  private readonly api = inject(Api);

  private readonly queue = inject(Queue);

  protected readonly icons = { CircleCheck };

  protected readonly fmt = { ago, clockAt, dayLabel, methodLabel, phone, rupees };

  protected readonly zone = NEPAL;

  protected readonly tabs: { value: Tab; label: string }[] = [
    { value: 'due', label: 'Due now' },
    { value: 'waiting', label: 'Waiting' },
    { value: 'sent', label: 'Sent' },
  ];

  protected readonly tab = signal<Tab>('due');

  protected readonly requested = signal<Withdrawal[] | null>(null);

  protected readonly sent = signal<Withdrawal[] | null>(null);

  protected readonly sending = signal<string | null>(null);

  protected readonly busy = signal<string | null>(null);

  protected readonly rowError = signal<{ id: string; message: string } | null>(null);

  protected readonly error = signal<string | null>(null);

  protected readonly shown = computed(() => {
    switch (this.tab()) {
      case 'due':
        return this.requested()?.filter((w) => this.isDue(w)) ?? null;
      case 'waiting':
        return this.requested()?.filter((w) => !this.isDue(w)) ?? null;
      case 'sent':
        return this.sent();
    }
  });

  constructor() {
    void this.loadRequested();
    effect(() => {
      if (this.tab() === 'sent') {
        untracked(() => void this.loadSent());
      }
    });
  }

  protected isDue(withdrawal: Withdrawal): boolean {
    return new Date(withdrawal.dueAt).getTime() <= Date.now();
  }

  protected count(tab: Tab): number {
    return (this.requested() ?? []).filter((w) => (tab === 'due') === this.isDue(w)).length;
  }

  protected localDay(instant: string): string {
    return today(NEPAL, new Date(instant));
  }

  protected async paid(withdrawal: Withdrawal, reference: string): Promise<void> {
    if (this.busy()) {
      return;
    }
    this.busy.set(withdrawal.id);
    this.rowError.set(null);
    try {
      await this.api.withdrawalPaid(withdrawal.id, reference);
      this.sending.set(null);
      this.requested.update((list) => list?.filter((w) => w.id !== withdrawal.id) ?? null);
      this.sent.set(null);
      void this.queue.refresh();
    }
    catch (error) {
      this.rowError.set({ id: withdrawal.id, message: problemOf(error).message });
    }
    finally {
      this.busy.set(null);
    }
  }

  private async loadRequested(): Promise<void> {
    try {
      this.requested.set(await this.api.withdrawals('REQUESTED'));
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }

  private async loadSent(): Promise<void> {
    try {
      this.sent.set((await this.api.withdrawals('PAID')).reverse());
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }
}
