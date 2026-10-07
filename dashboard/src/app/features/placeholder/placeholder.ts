import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { map } from 'rxjs';

import { NavItem } from '../../layout/navigation';

/** Stands in for screens that are planned but not built yet, so navigation can be tried end to end. */
@Component({
  selector: 'fs-placeholder',
  imports: [LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (item(); as item) {
      <div class="mx-auto max-w-5xl px-4 py-6 lg:px-8 lg:py-8">
        <h1 class="text-xl font-semibold tracking-tight lg:text-2xl">{{ item.label }}</h1>
        <div class="mt-6 flex flex-col items-center rounded-xl border border-dashed border-border bg-surface px-6 py-16 text-center">
          <span class="grid size-12 place-items-center rounded-full bg-surface-2 text-muted">
            <lucide-icon [img]="item.icon" [size]="22" />
          </span>
          <p class="mt-4 font-medium">Coming soon</p>
          <p class="mt-1 max-w-sm text-muted">{{ item.about }}</p>
        </div>
      </div>
    }
  `,
})
export class Placeholder {
  protected readonly item = toSignal(inject(ActivatedRoute).data.pipe(map((data) => data['nav'] as NavItem)));
}
