import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { MapPin, LucideAngularModule, Users } from 'lucide-angular';

import { MatchPost } from '../../core/api.types';
import { dayLabel, localClock, rupees } from '../../shared/format';
import { SKILL } from '../../shared/labels';

/** What a game post asks for, where and when: shared by the list and the post's page. */
@Component({
  selector: 'fs-match-summary',
  imports: [LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @let p = post();
    <div class="flex flex-wrap items-center gap-2">
      <span class="rounded-full px-2 py-0.5 text-xs font-medium" [class]="p.type === 'NEED_PLAYERS' ? 'bg-confirmed-bg text-confirmed-fg' : 'bg-tournament-bg text-tournament-fg'">
        {{ p.type === 'NEED_PLAYERS' ? 'Players wanted' : 'Opponent wanted' }}
      </span>
      @if (p.status !== 'OPEN') {
        <span class="rounded-full bg-surface-2 px-2 py-0.5 text-xs font-medium text-muted">{{ status() }}</span>
      }
      @if (p.skillLevel) {
        <span class="text-xs text-muted">{{ skill[p.skillLevel] }}</span>
      }
    </div>
    <p class="mt-2 text-lg font-semibold">{{ headline() }}</p>
    <p class="tabular text-muted">{{ fmt.dayLabel(p.startsAt.slice(0, 10)) }} · {{ fmt.localClock(p.startsAt) }}</p>
    <p class="mt-1 flex items-center gap-1 text-sm text-muted">
      <lucide-icon [img]="icons.MapPin" [size]="14" />{{ p.venue?.name ?? p.city ?? 'Place to be agreed' }}
    </p>
    <p class="mt-1 flex items-center gap-1 text-sm text-muted">
      <lucide-icon [img]="icons.Users" [size]="14" />{{ p.team ? p.team.name : p.postedBy.name }}
      @if (p.costPerPlayer) { · <span class="tabular">{{ fmt.rupees(p.costPerPlayer) }} each</span> }
    </p>
  `,
})
export class MatchSummary {
  readonly post = input.required<MatchPost>();

  protected readonly icons = { MapPin, Users };

  protected readonly fmt = { dayLabel, localClock, rupees };

  protected readonly skill = SKILL;

  protected readonly headline = computed(() => {
    const p = this.post();
    if (p.type === 'FIND_OPPONENT') {
      return `${p.team?.name ?? p.postedBy.name} want a game`;
    }
    const left = Math.max(0, (p.playersNeeded ?? 0) - p.placesTaken);
    return left === 0 ? 'Full' : `${left} ${left === 1 ? 'player' : 'players'} needed`;
  });

  protected readonly status = computed(() => {
    switch (this.post().status) {
      case 'FILLED':
        return 'Full';
      case 'CANCELLED':
        return 'Called off';
      case 'EXPIRED':
        return 'Over';
      default:
        return 'Open';
    }
  });
}
