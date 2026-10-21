import { HttpInterceptorFn } from '@angular/common/http';
import { signal } from '@angular/core';

import { en, Strings } from './en';
import { ne } from './ne';

/**
 * The app's words, numbers and dates in the player's language: English, or Nepali with Devanagari digits and
 * Bikram Sambat dates.
 *
 * {@link l} and the helpers in shared/format read a signal, so a template or computed() that shows them redraws
 * when the language changes. Text a component stored earlier (a message after saving) keeps the old words.
 */

export type Language = 'en' | 'ne';

export type { Strings };

const KEY = 'futslot.language';

const language = signal<Language>(saved() ?? (navigator.language.toLowerCase().startsWith('ne') ? 'ne' : 'en'));

document.documentElement.lang = language();

/** The language the app is showing. */
export const currentLanguage = language.asReadonly();

/** The words in the language the app is showing. */
export function l(): Strings {
  return language() === 'ne' ? ne : en;
}

/** Whether the app is in Nepali: Devanagari digits and Bikram Sambat dates. */
export function inNepali(): boolean {
  return language() === 'ne';
}

/** Writes the digits in {@code text} in Devanagari when the app is in Nepali: "6:30" → "६:३०". */
export function digits(text: number | string): string {
  return inNepali() ? String(text).replace(/\d/g, (digit) => '०१२३४५६७८९'[Number(digit)]) : String(text);
}

/** Whether the player picked a language in this browser, rather than it following the browser's. */
export function languageChosen(): boolean {
  return saved() !== null;
}

/** Switches the app's language; {@code remember} keeps it for the next visit. */
export function setLanguage(next: Language, remember = true): void {
  language.set(next);
  document.documentElement.lang = next;
  if (remember) {
    try {
      localStorage.setItem(KEY, next);
    } catch {
      // Applies for this visit only
    }
  }
}

/** Asks the API to answer in the app's language: its error messages are shown to the player as they come. */
export const languageInterceptor: HttpInterceptorFn = (request, next) =>
  request.url.startsWith('/api/') ? next(request.clone({ setHeaders: { 'Accept-Language': language() } })) : next(request);

function saved(): Language | null {
  try {
    const value = localStorage.getItem(KEY);
    return value === 'en' || value === 'ne' ? value : null;
  } catch {
    return null;
  }
}
