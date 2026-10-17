import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { PlayerCard } from '../core/api.types';
import { FOOT, POSITION, SKILL } from './labels';

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
        <p class="text-sm text-muted">{{ details() || 'No profile yet' }}</p>
        @if (p.bio) {
          <p class="mt-1 text-sm">{{ p.bio }}</p>
        }
      </div>
    </div>
  `,
})
export class PlayerCardView {
  readonly player = input.required<PlayerCard>();

  protected details(): string {
    const p = this.player();
    return [
      p.position ? POSITION[p.position] : null,
      p.skillLevel ? SKILL[p.skillLevel] : null,
      p.preferredFoot ? `${FOOT[p.preferredFoot]} foot` : null,
      p.city,
      `${p.gamesPlayed} ${p.gamesPlayed === 1 ? 'game' : 'games'}`,
    ].filter(Boolean).join(' · ');
  }
}
