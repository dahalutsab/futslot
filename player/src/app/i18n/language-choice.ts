import { effect, inject, Injectable, untracked } from '@angular/core';

import { Api } from '../core/api';
import { Session } from '../core/session';
import { currentLanguage, Language, languageChosen, setLanguage } from './i18n';

/**
 * Keeps the account's language (`preferredLanguage`, which notifications and texts use) in step with the app's. A
 * browser where the player hasn't picked a language takes the account's Nepali at sign-in; one where they have tells
 * the account.
 */
@Injectable({ providedIn: 'root' })
export class LanguageChoice {
  private readonly api = inject(Api);

  private readonly session = inject(Session);

  constructor() {
    let signedInAs: string | null = null;
    effect(() => {
      const user = this.session.user();
      const language = currentLanguage();
      untracked(() => {
        const newSignIn = user !== null && user.id !== signedInAs;
        signedInAs = user?.id ?? null;
        if (newSignIn && !languageChosen() && user.preferredLanguage === 'ne' && language !== 'ne') {
          setLanguage('ne');
        } else {
          void this.tellAccount();
        }
      });
    });
  }

  /** The player picked {@code language}: switches straight away, and the account follows. */
  set(language: Language): void {
    setLanguage(language);
  }

  private async tellAccount(): Promise<void> {
    const user = this.session.user();
    const language = currentLanguage();
    if (!user || user.preferredLanguage === language) {
      return;
    }
    try {
      this.session.updateUser(await this.api.updateMe({ preferredLanguage: language }));
    } catch {
      // Tried again next time; the app's own language doesn't depend on it
    }
  }
}
