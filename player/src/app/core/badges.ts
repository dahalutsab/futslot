import { effect, inject, Injectable, signal, untracked } from '@angular/core';

import { Api } from './api';
import { Session } from './session';

const POLL_MS = 60_000;

/** Unread notifications and messages, for the counts in the bars. Checked every minute while the tab is in view. */
@Injectable({ providedIn: 'root' })
export class Badges {
  private readonly api = inject(Api);

  private readonly session = inject(Session);

  readonly notifications = signal(0);

  readonly messages = signal(0);

  constructor() {
    effect(() => {
      if (this.session.signedIn()) {
        untracked(() => void this.refresh());
      } else {
        this.notifications.set(0);
        this.messages.set(0);
      }
    });
    setInterval(() => {
      if (this.session.signedIn() && document.visibilityState === 'visible') {
        void this.refresh();
      }
    }, POLL_MS);
  }

  async refresh(): Promise<void> {
    try {
      const [notifications, conversations] = await Promise.all([this.api.unreadNotifications(), this.api.conversations()]);
      this.notifications.set(notifications);
      this.messages.set(conversations.reduce((sum, c) => sum + c.unread, 0));
    }
    catch {
      // Counts catch up on the next check
    }
  }
}
