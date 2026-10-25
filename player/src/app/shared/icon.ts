import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

import { IconData } from './icon-data';

export * from './icon-data';

/**
 * A Phosphor duotone icon (docs/design/broadsheet.md) in the text's colour. {@code size} is in pixels; a
 * {@code fill-current} class fills the tinted half in too, for a chosen star or a saved heart.
 */
@Component({
  selector: 'fs-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'inline-flex shrink-0', 'aria-hidden': 'true', '[innerHTML]': 'svg()' },
  template: '',
})
export class Icon {
  readonly img = input.required<IconData>();

  readonly size = input(24);

  /** Not used: Phosphor's strokes are drawn into the shapes. Kept so callers can say how heavy they meant. */
  readonly strokeWidth = input<number>();

  private readonly sanitizer = inject(DomSanitizer);

  // The markup is this app's own, generated from Phosphor: nothing a user typed
  protected readonly svg = computed(() => this.sanitizer.bypassSecurityTrustHtml(
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256" width="${this.size()}" height="${this.size()}" fill="currentColor">${this.img()}</svg>`));
}
