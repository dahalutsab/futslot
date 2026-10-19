import { afterNextRender, ChangeDetectionStrategy, Component, ElementRef, input, output, viewChild } from '@angular/core';

/** The bits of Google Identity Services this uses. */
interface GoogleId {
  initialize(config: { client_id: string; callback: (response: { credential: string }) => void }): void;
  renderButton(parent: HTMLElement, options: Record<string, string | number>): void;
}

declare global {
  interface Window {
    google?: { accounts: { id: GoogleId } };
  }
}

let script: Promise<GoogleId> | null = null;

/** Loads Google's sign-in script once, on first use. */
function loadGoogle(): Promise<GoogleId> {
  script ??= new Promise<GoogleId>((resolve, reject) => {
    const tag = document.createElement('script');
    tag.src = 'https://accounts.google.com/gsi/client';
    tag.async = true;
    tag.onload = () => (window.google ? resolve(window.google.accounts.id) : reject(new Error('No Google')));
    tag.onerror = () => {
      script = null;
      reject(new Error('Google sign-in did not load'));
    };
    document.head.appendChild(tag);
  });
  return script;
}

/**
 * Google's own "Continue with Google" button (its look is Google's rule). Emits the ID token to send to the API.
 * Shows nothing if Google's script can't load, so the phone and password form is always there to fall back on.
 */
@Component({
  selector: 'fs-google-button',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div #host class="flex min-h-11 justify-center"></div>`,
})
export class GoogleButton {
  readonly clientId = input.required<string>();

  readonly credential = output<string>();

  private readonly host = viewChild.required<ElementRef<HTMLElement>>('host');

  constructor() {
    afterNextRender(() => {
      loadGoogle().then((google) => {
        const host = this.host().nativeElement;
        google.initialize({ client_id: this.clientId(), callback: (response) => this.credential.emit(response.credential) });
        google.renderButton(host, {
          type: 'standard',
          theme: document.documentElement.classList.contains('dark') ? 'filled_black' : 'outline',
          size: 'large',
          text: 'continue_with',
          shape: 'rectangular',
          width: Math.min(host.clientWidth || 384, 400),
        });
      }, () => undefined);
    });
  }
}
