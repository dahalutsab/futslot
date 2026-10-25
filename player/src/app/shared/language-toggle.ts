import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';

import { currentLanguage, Language } from '../i18n/i18n';
import { LanguageChoice } from '../i18n/language-choice';

/** Each language named in itself, so whoever can't read the other one still finds theirs. */
export const LANGUAGE_NAMES: Record<Language, string> = { en: 'English', ne: 'नेपाली' };

/**
 * A top-bar button for the other language, for when the player isn't signed in (signed in, it's under Me).
 * {@code onTurf} draws it as a white outline, for the turf top bar.
 */
@Component({
  selector: 'fs-language-toggle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button type="button" (click)="choice.set(other())" [attr.lang]="other()"
      [class]="onTurf() ? 'h-9 rounded-full border border-white/40 px-3.5 text-[15px] text-white hover:border-white hover:bg-white/10'
        : 'h-10 rounded-md px-3 font-medium text-muted hover:bg-surface-2 hover:text-text'">{{ names[other()] }}</button>
  `,
})
export class LanguageToggle {
  readonly onTurf = input(false);

  protected readonly choice = inject(LanguageChoice);

  protected readonly names = LANGUAGE_NAMES;

  protected other(): Language {
    return currentLanguage() === 'en' ? 'ne' : 'en';
  }
}
