import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * FutSlot's name as the masthead: set in the serif, no mark (docs/design/broadsheet.md). {@code name} is what it
 * reads ("FutSlot admin"). Its size is the caller's: a text size class on the element.
 */
@Component({
  selector: 'fs-logo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'inline-flex shrink-0 items-baseline font-display font-semibold leading-none tracking-[-0.01em] text-text' },
  template: `{{ name() }}`,
})
export class Logo {
  readonly name = input('FutSlot');
}
