import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { CheckCheck, CircleCheck, CircleX, Clock, Flag, LucideAngularModule, LucideIconData, TimerOff, Trophy, Wallet } from 'lucide-angular';

export type ChipState = 'confirmed' | 'held' | 'verify' | 'completed' | 'noshow' | 'cancelled' | 'expired' | 'tournament';

const STATES: Record<ChipState, { label: string; icon: LucideIconData; classes: string }> = {
  confirmed: { label: 'Confirmed', icon: CircleCheck, classes: 'bg-confirmed-bg text-confirmed-fg' },
  held: { label: 'Waiting for payment', icon: Clock, classes: 'bg-held-bg text-held-fg' },
  verify: { label: 'Payment being checked', icon: Wallet, classes: 'bg-verify-bg text-verify-fg' },
  completed: { label: 'Played', icon: CheckCheck, classes: 'bg-surface-2 text-muted' },
  noshow: { label: 'No-show', icon: Flag, classes: 'bg-noshow-bg text-noshow-fg' },
  cancelled: { label: 'Cancelled', icon: CircleX, classes: 'bg-surface-2 text-muted' },
  expired: { label: 'Not paid in time', icon: TimerOff, classes: 'bg-surface-2 text-muted' },
  tournament: { label: 'Tournament', icon: Trophy, classes: 'bg-tournament-bg text-tournament-fg' },
};

/** A state's colour, always with its icon and words (colour alone isn't enough). */
@Component({
  selector: 'fs-status-chip',
  imports: [LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="inline-flex h-6 items-center gap-1 rounded-full px-2 text-xs font-medium" [class]="state().classes">
      <lucide-icon [img]="state().icon" [size]="13" [strokeWidth]="2" />
      {{ state().label }}
    </span>
  `,
})
export class StatusChip {
  readonly value = input.required<ChipState>();

  protected readonly state = computed(() => STATES[this.value()]);
}

export function stateIcon(state: ChipState): LucideIconData {
  return STATES[state].icon;
}

export function stateLabel(state: ChipState): string {
  return STATES[state].label;
}
