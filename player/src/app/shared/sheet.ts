import { A11yModule } from '@angular/cdk/a11y';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { Icon, X } from './icon';

import { l } from '../i18n/i18n';

/**
 * A panel for one task without leaving the screen: from the right on wide screens, from the bottom on
 * phones. Escape, the close button or tapping outside closes it; focus stays inside while open.
 */
@Component({
  selector: 'fs-sheet',
  imports: [A11yModule, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'closed.emit()' },
  template: `
    <div class="fixed inset-0 z-40 bg-black/50" (click)="closed.emit()" aria-hidden="true"></div>
    <section role="dialog" aria-modal="true" [attr.aria-label]="title()" cdkTrapFocus cdkTrapFocusAutoCapture
      class="fixed inset-x-0 bottom-0 z-50 flex max-h-[92dvh] flex-col rounded-t-2xl bg-surface shadow-xl
             lg:inset-y-0 lg:left-auto lg:right-0 lg:max-h-none lg:w-[440px] lg:rounded-none lg:border-l lg:border-border">
      <header class="flex items-start gap-3 border-b border-border px-5 py-4">
        <div class="min-w-0 flex-1">
          <h2 class="text-lg font-semibold leading-tight">{{ title() }}</h2>
          @if (subtitle()) {
            <p class="mt-0.5 text-muted">{{ subtitle() }}</p>
          }
        </div>
        <button type="button" (click)="closed.emit()" [attr.aria-label]="l().close"
          class="-mr-2 grid size-11 shrink-0 place-items-center rounded-md text-muted hover:bg-surface-2 hover:text-text">
          <fs-icon [img]="close" [size]="20" />
        </button>
      </header>
      <div class="flex-1 overflow-y-auto px-5 py-4">
        <ng-content />
      </div>
      <ng-content select="[sheet-footer]" />
    </section>
  `,
})
export class Sheet {
  protected readonly l = l;

  readonly title = input.required<string>();

  readonly subtitle = input<string | null>(null);

  readonly closed = output<void>();

  protected readonly close = X;
}
