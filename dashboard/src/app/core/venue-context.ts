import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { MyVenue, VenuePermission } from './api.types';

const SELECTED_KEY = 'futslot.venue';

/**
 * The venues the user runs or works at, and which one they're looking at. Everything in the dashboard is
 * for the selected venue, and navigation only shows what their permissions there allow.
 */
@Injectable({ providedIn: 'root' })
export class VenueContext {
  private readonly http = inject(HttpClient);

  readonly venues = signal<MyVenue[] | null>(null);

  private readonly selectedId = signal<string | null>(readSelected());

  readonly current = computed<MyVenue | null>(() => {
    const venues = this.venues();
    if (!venues || venues.length === 0) {
      return null;
    }
    return venues.find((entry) => entry.venue.id === this.selectedId()) ?? venues[0];
  });

  async load(): Promise<MyVenue[]> {
    const venues = await firstValueFrom(this.http.get<MyVenue[]>('/api/v1/owner/venues'));
    this.venues.set(venues);
    return venues;
  }

  select(venueId: string): void {
    this.selectedId.set(venueId);
    try {
      localStorage.setItem(SELECTED_KEY, venueId);
    } catch {
      // Not remembered across reloads
    }
  }

  can(permission: VenuePermission): boolean {
    return this.current()?.permissions.includes(permission) ?? false;
  }

  isOwner(): boolean {
    return this.current()?.access === 'OWNER';
  }
}

function readSelected(): string | null {
  try {
    return localStorage.getItem(SELECTED_KEY);
  } catch {
    return null;
  }
}
