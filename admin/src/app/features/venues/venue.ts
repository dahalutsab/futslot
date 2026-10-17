import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Api } from '../../core/api';
import { problemOf } from '../../core/api-error';
import { AuditEntry, VenueDetail } from '../../core/api.types';
import { Queue } from '../../core/queue';
import { ago, clock, phone, rupees } from '../../shared/format';
import { actionLabel, VENUE_STATUS } from '../../shared/labels';
import { NoteForm } from '../../shared/note-form';

const WEEK = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'] as const;

/**
 * Everything about one venue, to decide whether it goes live: details, courts and prices, hours, rules and
 * photos. Approve or reject one waiting for approval; suspend a live one or bring it back. Owners are told why.
 */
@Component({
  selector: 'fs-venue',
  imports: [RouterLink, NoteForm],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './venue.html',
})
export class VenuePage {
  private readonly api = inject(Api);

  private readonly queue = inject(Queue);

  readonly id = input.required<string>();

  protected readonly fmt = { rupees, phone, ago, actionLabel };

  protected readonly label = VENUE_STATUS;

  protected readonly reasonLabel = {
    reject: "Why isn't it ready? The owner sees this.",
    suspend: 'Why suspend it? The owner sees this.',
  };

  protected readonly amenityName = (amenity: { name: string }) => amenity.name;

  protected readonly venue = signal<VenueDetail | null>(null);

  protected readonly history = signal<AuditEntry[]>([]);

  /** The action whose reason is being written. */
  protected readonly refusing = signal<'reject' | 'suspend' | null>(null);

  protected readonly busy = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly hours = computed(() => {
    const venue = this.venue();
    return WEEK.map((day) => {
      const h = venue?.operatingHours.find((o) => o.day === day);
      const label = day.charAt(0) + day.slice(1).toLowerCase();
      return { day: label, hours: !h || h.closed || !h.openTime || !h.closeTime ? 'Closed' : `${time(h.openTime)} – ${time(h.closeTime)}` };
    });
  });

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => void this.load(id));
    });
  }

  protected approve(): Promise<void> {
    return this.act((v) => this.api.approve(v.id));
  }

  protected refuse(reason: string): Promise<void> {
    const action = this.refusing();
    return action ? this.act((v) => this.api.refuse(v.id, action, reason)) : Promise.resolve();
  }

  protected reinstate(): Promise<void> {
    return this.act((v) => this.api.reinstate(v.id));
  }

  protected changes(entry: AuditEntry): string {
    return Object.entries(entry.changes ?? {}).map(([key, value]) => `${key}: ${value}`).join(' · ');
  }

  private async act(action: (venue: VenueDetail) => Promise<VenueDetail>): Promise<void> {
    const venue = this.venue();
    if (!venue || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      this.venue.set(await action(venue));
      this.refusing.set(null);
      void this.queue.refresh();
      void this.loadHistory(venue.id);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
    finally {
      this.busy.set(false);
    }
  }

  private async load(id: string): Promise<void> {
    try {
      this.venue.set(await this.api.venue(id));
      void this.loadHistory(id);
    }
    catch (error) {
      this.error.set(problemOf(error).message);
    }
  }

  private async loadHistory(id: string): Promise<void> {
    try {
      this.history.set((await this.api.audit(id)).content.slice(0, 10));
    }
    catch {
      // The history is extra; the venue still shows
    }
  }
}

function time(value: string): string {
  const [h, m] = value.split(':').map(Number);
  return clock(h * 60 + m);
}
