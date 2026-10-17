import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

/** Switches between the parts of Play: pickup games, tournaments and teams. */
@Component({
  selector: 'fs-play-nav',
  imports: [RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav class="flex gap-1 rounded-lg bg-surface-2 p-1" aria-label="Play">
      @for (link of links; track link.path) {
        <a [routerLink]="link.path" routerLinkActive="bg-surface text-text" ariaCurrentWhenActive="page"
          class="flex h-10 flex-1 items-center justify-center rounded-md font-medium text-muted">{{ link.label }}</a>
      }
    </nav>
  `,
})
export class PlayNav {
  protected readonly links = [
    { path: '/matches', label: 'Games' },
    { path: '/tournaments', label: 'Tournaments' },
    { path: '/teams', label: 'Teams' },
  ];
}
