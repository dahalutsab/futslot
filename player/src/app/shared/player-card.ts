import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { PlayerCard } from '../core/api.types';
import { l } from '../i18n/i18n';
import { footLabels, positionLabels, skillLabels } from './labels';

/** A player's name and what they play, for match requests and squads. */
@Component({
  selector: 'fs-player-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @let p = player();
    <div class="flex items-start gap-3">
      <span class="grid size-10 shrink-0 place-items-center rounded-full bg-surface-2 font-semibold text-muted" aria-hidden="true">{{ p.name.charAt(0).toUpperCase() }}</span>
      <div class="min-w-0 flex-1">
        <p class="font-medium">{{ p.name }}</p>
        <p class="text-sm text-muted">{{ details() || l().noProfileYet }}</p>
        @if (p.bio) {
          <p class="mt-1 text-sm">{{ p.bio }}</p>
        }
      </div>
    </div>
  `,
})
export class PlayerCardView {
  protected readonly l = l;

  readonly player = input.required<PlayerCard>();

  protected details(): string {
    const p = this.player();
    return [
      p.position ? positionLabels()[p.position] : null,
      p.skillLevel ? skillLabels()[p.skillLevel] : null,
      p.preferredFoot ? l().footSide(footLabels()[p.preferredFoot]) : null,
      p.city,
      l().games(p.gamesPlayed),
    ].filter(Boolean).join(' · ');
  }
}
