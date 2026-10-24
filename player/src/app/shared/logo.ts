import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** FutSlot's mark (a pitch from above) and name. {@code onTurf} draws it white, for the turf band. */
@Component({
  selector: 'fs-logo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex items-center gap-2.5' },
  template: `
    <svg viewBox="0 0 30 20" class="h-5 w-[30px] shrink-0" [class]="onTurf() ? 'text-white' : 'text-brand'" aria-hidden="true">
      <rect width="30" height="20" rx="3" fill="currentColor" />
      <g [attr.stroke]="onTurf() ? '#11673a' : '#fff'" stroke-width="1.4" fill="none">
        <path d="M15 2.5v15M2.5 6.5h3a3.5 3.5 0 0 1 0 7h-3M27.5 6.5h-3a3.5 3.5 0 0 0 0 7h3" />
        <circle cx="15" cy="10" r="3.2" />
      </g>
    </svg>
    <span class="font-display text-[26px] font-bold leading-none">FutSlot</span>
  `,
})
export class Logo {
  readonly onTurf = input(false);
}
