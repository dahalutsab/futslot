import { effect, inject, Injectable, signal, untracked } from '@angular/core';

import { Api } from './api';
import { Session } from './session';

const POLL_MS = 60_000;

/** What's waiting on an admin, for the counts in the navigation and on the overview. */
export interface QueueCounts {
  venues: number;
  payments: number;
  /** Withdrawals whose 3-day wait is over, so they can be sent now. */
  withdrawals: number;
  payouts: number;
}

const EMPTY: QueueCounts = { venues: 0, payments: 0, withdrawals: 0, payouts: 0 };

/** Counts of work waiting for an admin, checked every minute while the tab is in view. */
@Injectable({ providedIn: 'root' })
export class Queue {
  private readonly api = inject(Api);

  private readonly session = inject(Session);

  readonly counts = signal<QueueCounts>(EMPTY);

  constructor() {
    effect(() => {
      if (this.session.admin()) {
        untracked(() => void this.refresh());
      } else {
        this.counts.set(EMPTY);
      }
    });
    setInterval(() => {
      if (this.session.admin() && document.visibilityState === 'visible') {
        void this.refresh();
      }
    }, POLL_MS);
  }

  async refresh(): Promise<void> {
    try {
      const [venues, payments, withdrawals, payouts] = await Promise.all([
        this.api.venues('PENDING_APPROVAL'), this.api.pendingPayments(), this.api.withdrawals('REQUESTED'), this.api.payouts(),
      ]);
      const now = Date.now();
      this.counts.set({
        venues: venues.totalElements,
        payments: payments.length,
        withdrawals: withdrawals.filter((w) => new Date(w.dueAt).getTime() <= now).length,
        payouts: payouts.length,
      });
    }
    catch {
      // Counts catch up on the next check
    }
  }
}
