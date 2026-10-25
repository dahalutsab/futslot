import { Injectable, signal } from '@angular/core';

export type ThemeChoice = 'light' | 'dark' | 'system';

const KEY = 'futslot.theme';

/**
 * Follows the device's light or dark setting unless the player picks one. Shared by key with FutSlot's public pages
 * on the same address (site-assets/futslot.css), so the front page and the app are the same edition.
 */
@Injectable({ providedIn: 'root' })
export class Theme {
  readonly choice = signal<ThemeChoice>(readChoice());

  /** Missing in some embedded browsers and in tests; then "Auto" means light. */
  private readonly media = typeof matchMedia === 'function' ? matchMedia('(prefers-color-scheme: dark)') : null;

  constructor() {
    this.media?.addEventListener('change', () => this.apply());
    this.apply();
  }

  set(choice: ThemeChoice): void {
    this.choice.set(choice);
    try {
      localStorage.setItem(KEY, choice);
    } catch {
      // Applies for this visit only
    }
    this.apply();
  }

  private apply(): void {
    const choice = this.choice();
    const dark = choice === 'dark' || (choice === 'system' && (this.media?.matches ?? false));
    document.documentElement.classList.toggle('dark', dark);
    // The browser's own bar follows the page, not only the device setting
    document.querySelectorAll('meta[name="theme-color"]').forEach((meta) => meta.setAttribute('content', dark ? '#151413' : '#f3f2f2'));
  }
}

function readChoice(): ThemeChoice {
  try {
    const saved = localStorage.getItem(KEY);
    return saved === 'light' || saved === 'dark' ? saved : 'system';
  } catch {
    return 'system';
  }
}
