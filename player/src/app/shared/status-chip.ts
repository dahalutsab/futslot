import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { CheckCheck, CircleCheck, CircleX, Clock, Flag, LucideAngularModule, LucideIconData, TimerOff, Trophy, Wallet } from 'lucide-angular';

import { l, Strings } from '../i18n/i18n';

export type ChipState = 'confirmed' | 'held' | 'verify' | 'completed' | 'noshow' | 'cancelled' | 'expired' | 'tournament';

const STATES: Record<ChipState, { label: (words: Strings) => string; icon: LucideIconData; classes: string }> = {
  confirmed: { label: (w) => w.chipConfirmed, icon: CircleCheck, classes: 'bg-confirmed-bg text-confirmed-fg' },
  held: { label: (w) => w.chipHeld, icon: Clock, classes: 'bg-held-bg text-held-fg' },
  verify: { label: (w) => w.chipVerify, icon: Wallet, classes: 'bg-verify-bg text-verify-fg' },
  completed: { label: (w) => w.chipCompleted, icon: CheckCheck, classes: 'bg-surface-2 text-muted' },
  noshow: { label: (w) => w.chipNoShow, icon: Flag, classes: 'bg-noshow-bg text-noshow-fg' },
  cancelled: { label: (w) => w.chipCancelled, icon: CircleX, classes: 'bg-surface-2 text-muted' },
  expired: { label: (w) => w.chipExpired, icon: TimerOff, classes: 'bg-surface-2 text-muted' },
  tournament: { label: (w) => w.chipTournament, icon: Trophy, classes: 'bg-tournament-bg text-tournament-fg' },
};

/** A state's colour, always with its icon and words (colour alone isn't enough). */
@Component({
  selector: 'fs-status-chip',
  imports: [LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="inline-flex h-6 items-center gap-1 rounded-full px-2 text-xs font-medium" [class]="state().classes">
      <lucide-icon [img]="state().icon" [size]="13" [strokeWidth]="2" />
      {{ stateLabel(value()) }}
    </span>
  `,
})
export class StatusChip {
  readonly value = input.required<ChipState>();

  protected readonly state = computed(() => STATES[this.value()]);

  protected readonly stateLabel = stateLabel;
}

export function stateIcon(state: ChipState): LucideIconData {
  return STATES[state].icon;
}

export function stateLabel(state: ChipState): string {
  return STATES[state].label(l());
}
