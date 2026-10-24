import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * FutSlot's mark (a pitch from above) and name, the same in every app. {@code onTurf} draws it white, for the turf
 * band; {@code name} is what follows the mark ("FutSlot admin"), and an empty one shows the mark alone.
 */
@Component({
  selector: 'fs-logo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex shrink-0 items-center gap-2.5' },
  template: `
    <svg viewBox="0 0 30 20" class="h-5 w-[30px] shrink-0" [class]="onTurf() ? 'text-white' : 'text-brand'" aria-hidden="true">
      <rect width="30" height="20" rx="3" fill="currentColor" />
      <g [attr.stroke]="onTurf() ? '#11673a' : '#fff'" stroke-width="1.4" fill="none">
        <path d="M15 2.5v15M2.5 6.5h3a3.5 3.5 0 0 1 0 7h-3M27.5 6.5h-3a3.5 3.5 0 0 0 0 7h3" />
        <circle cx="15" cy="10" r="3.2" />
      </g>
    </svg>
    @if (name()) {
      <span class="font-display text-[26px] font-bold leading-none">{{ name() }}</span>
    }
  `,
})
export class Logo {
  readonly onTurf = input(false);

  readonly name = input('FutSlot');
}
