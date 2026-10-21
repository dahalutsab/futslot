import { ChangeDetectionStrategy, Component, computed } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { l } from '../i18n/i18n';

/** Switches between the parts of Play: pickup games, tournaments and teams. */
@Component({
  selector: 'fs-play-nav',
  imports: [RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav class="flex gap-1 rounded-lg bg-surface-2 p-1" [attr.aria-label]="l().tabPlay">
      @for (link of links(); track link.path) {
        <a [routerLink]="link.path" routerLinkActive="bg-surface text-text" ariaCurrentWhenActive="page"
          class="flex h-10 flex-1 items-center justify-center rounded-md font-medium text-muted">{{ link.label }}</a>
      }
    </nav>
  `,
})
export class PlayNav {
  protected readonly l = l;

  protected readonly links = computed(() => [
    { path: '/matches', label: l().playGames },
    { path: '/tournaments', label: l().playTournaments },
    { path: '/teams', label: l().playTeams },
  ]);
}
