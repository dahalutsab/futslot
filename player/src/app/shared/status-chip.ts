import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { CheckCheck, CircleCheck, CircleX, Clock, Flag, Icon, IconData, TimerOff, Trophy, Wallet } from './icon';

import { l, Strings } from '../i18n/i18n';

export type ChipState = 'confirmed' | 'held' | 'verify' | 'completed' | 'noshow' | 'cancelled' | 'expired' | 'tournament';

const STATES: Record<ChipState, { label: (words: Strings) => string; icon: IconData; classes: string }> = {
  confirmed: { label: (w) => w.chipConfirmed, icon: CircleCheck, classes: 'bg-confirmed-bg text-confirmed-fg' },
  held: { label: (w) => w.chipHeld, icon: Clock, classes: 'bg-held-bg text-held-fg' },
  verify: { label: (w) => w.chipVerify, icon: Wallet, classes: 'bg-verify-bg text-verify-fg' },
  completed: { label: (w) => w.chipCompleted, icon: CheckCheck, classes: 'bg-neutral-100 text-neutral-800' },
  noshow: { label: (w) => w.chipNoShow, icon: Flag, classes: 'bg-noshow-bg text-noshow-fg' },
  cancelled: { label: (w) => w.chipCancelled, icon: CircleX, classes: 'bg-neutral-100 text-neutral-800' },
  expired: { label: (w) => w.chipExpired, icon: TimerOff, classes: 'bg-neutral-100 text-neutral-800' },
  tournament: { label: (w) => w.chipTournament, icon: Trophy, classes: 'bg-tournament-bg text-tournament-fg' },
};

/** A state's colour, always with its icon and words (colour alone isn't enough). */
@Component({
  selector: 'fs-status-chip',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="inline-flex items-center gap-1 rounded-[1.5px] px-2.5 py-[3px] text-xs tracking-[0.02em]" [class]="state().classes">
      <fs-icon [img]="state().icon" [size]="13" [strokeWidth]="2" />
      {{ stateLabel(value()) }}
    </span>
  `,
})
export class StatusChip {
  readonly value = input.required<ChipState>();

  protected readonly state = computed(() => STATES[this.value()]);

  protected readonly stateLabel = stateLabel;
}

export function stateIcon(state: ChipState): IconData {
  return STATES[state].icon;
}

export function stateLabel(state: ChipState): string {
  return STATES[state].label(l());
}
