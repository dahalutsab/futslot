import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';

import { problemOf } from '../../core/api-error';
import { CourtBlock } from '../../core/api.types';
import { Sheet } from '../../shared/sheet';
import { StatusChip } from '../../shared/status-chip';
import { ScheduleApi } from './schedule-api';

const REASONS: Record<string, string> = {
  MAINTENANCE: 'Maintenance',
  PRIVATE_EVENT: 'Private event',
  HOLIDAY: 'Closed (holiday)',
  TOURNAMENT: 'Tournament match',
  OTHER: 'Other',
};

/** Blocked court time. Tournament matches are moved from the tournament, not here. */
@Component({
  selector: 'fs-block-panel',
  imports: [Sheet, StatusChip],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <fs-sheet [title]="reasonLabel()" [subtitle]="courtName() + ', ' + range()" (closed)="closed.emit()">
      <fs-status-chip [value]="tournament() ? 'tournament' : 'blocked'" />
      @if (block().note) {
        <p class="mt-4 text-muted">{{ block().note }}</p>
      }
      @if (tournament()) {
        <p class="mt-4 text-sm text-subtle">Scheduled from Tournaments. Move or cancel the match there.</p>
      } @else if (canRemove()) {
        @if (error(); as message) {
          <p class="mt-4 rounded-lg bg-danger-bg px-3 py-2 text-danger" role="alert">{{ message }}</p>
        }
        <button type="button" (click)="remove()" [disabled]="busy()"
          class="mt-6 h-12 w-full rounded-lg border border-border font-medium hover:bg-surface-2 disabled:opacity-60">
          Remove block (open for booking)
        </button>
      }
    </fs-sheet>
  `,
})
export class BlockPanel {
  private readonly api = inject(ScheduleApi);

  readonly venueId = input.required<string>();

  readonly block = input.required<CourtBlock>();

  readonly courtName = input.required<string>();

  readonly timeZone = input.required<string>();

  readonly canRemove = input(false);

  readonly done = output<void>();

  readonly closed = output<void>();

  protected readonly busy = signal(false);

  protected readonly error = signal<string | null>(null);

  protected readonly tournament = computed(() => this.block().reason === 'TOURNAMENT');

  protected readonly reasonLabel = computed(() => REASONS[this.block().reason] ?? 'Blocked');

  protected readonly range = computed(() => {
    const time = new Intl.DateTimeFormat('en-US', { hour: 'numeric', minute: '2-digit', timeZone: this.timeZone() });
    return `${time.format(new Date(this.block().startsAt))}–${time.format(new Date(this.block().endsAt))}`;
  });

  protected async remove(): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      await this.api.unblock(this.venueId(), this.block().courtId, this.block().id);
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
