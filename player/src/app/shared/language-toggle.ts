import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { currentLanguage, Language } from '../i18n/i18n';
import { LanguageChoice } from '../i18n/language-choice';

/** Each language named in itself, so whoever can't read the other one still finds theirs. */
export const LANGUAGE_NAMES: Record<Language, string> = { en: 'English', ne: 'नेपाली' };

/**
 * The top bar's "EN · नेपाली" for a player who isn't signed in (signed in, it's under Me): the language in use in
 * full ink, the other one a button.
 */
@Component({
  selector: 'fs-language-toggle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'inline-flex items-center gap-1.5 text-[13px] tracking-[0.08em] text-text/70' },
  template: `
    @for (language of languages; track language; let last = $last) {
      @if (language === current()) {
        <span [attr.lang]="language" class="text-text">{{ labels[language] }}</span>
      } @else {
        <button type="button" (click)="choice.set(language)" [attr.lang]="language" [attr.aria-label]="names[language]"
          class="min-h-11 px-0.5 hover:text-accent-700">{{ labels[language] }}</button>
      }
      @if (!last) {
        <span aria-hidden="true">·</span>
      }
    }
  `,
})
export class LanguageToggle {
  protected readonly choice = inject(LanguageChoice);

  protected readonly names = LANGUAGE_NAMES;

  protected readonly languages: Language[] = ['en', 'ne'];

  protected readonly labels: Record<Language, string> = { en: 'EN', ne: 'नेपाली' };

  protected current(): Language {
    return currentLanguage();
  }
}
