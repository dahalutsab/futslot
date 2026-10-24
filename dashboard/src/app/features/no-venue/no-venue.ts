import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { Session } from '../../core/session';

/** Signed in, but not an owner or staff anywhere yet. */
@Component({
  selector: 'fs-no-venue',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex min-h-dvh flex-col items-center justify-center px-6 text-center">
      <h1 class="text-3xl">No venue yet</h1>
      <p class="mt-2 max-w-sm text-muted">
        {{ session.user()?.fullName }}, this number isn't the owner or staff of any venue. Ask the owner to add you
        as staff, or list your venue with FutSlot.
      </p>
      <button type="button" (click)="session.signOut()"
        class="mt-6 h-11 rounded-md border border-border px-5 font-medium hover:bg-surface-2">Use another number</button>
    </div>
  `,
})
export class NoVenue {
  protected readonly session = inject(Session);
}
