import { HttpClient } from '@angular/common/http';
import { DestroyRef, effect, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { VenueContext } from '../core/venue-context';

/** Counts on the Payments and Messages nav items, refreshed every minute and when the venue changes. */
@Injectable({ providedIn: 'root' })
export class Badges {
  private readonly http = inject(HttpClient);

  private readonly venues = inject(VenueContext);

  readonly payments = signal(0);

  readonly messages = signal(0);

  constructor() {
    const timer = setInterval(() => void this.refresh(), 60_000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
    effect(() => {
      if (this.venues.current()) {
        void this.refresh();
      }
    });
  }

  async refresh(): Promise<void> {
    const current = this.venues.current();
    if (!current || !current.permissions.includes('MANAGE_BOOKINGS')) {
      this.payments.set(0);
      this.messages.set(0);
      return;
    }
    const base = `/api/v1/owner/venues/${current.venue.id}`;
    try {
      const [pending, conversations] = await Promise.all([
        firstValueFrom(this.http.get<unknown[]>(`${base}/payments/pending`)),
        firstValueFrom(this.http.get<{ unread: number }[]>(`${base}/conversations`)),
      ]);
      this.payments.set(pending.length);
      this.messages.set(conversations.reduce((sum, conversation) => sum + conversation.unread, 0));
    }
    catch {
      // Keep the last counts; the next refresh will try again
    }
  }
}
