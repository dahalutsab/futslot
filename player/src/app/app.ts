import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { Theme } from './core/theme';
import { LanguageChoice } from './i18n/language-choice';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '<router-outlet />',
})
export class App {
  constructor() {
    // Follows the device's light/dark setting from the start, and keeps the account's language in step
    inject(Theme);
    inject(LanguageChoice);
  }
}
