import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom, Observable, shareReplay, tap, finalize } from 'rxjs';

import { AuthOptions, CodePurpose, OtpSent, TokenResponse, User } from './api.types';

const STORAGE_KEY = 'futslot.session';

interface StoredSession {
  accessToken: string;
  refreshToken: string;
  user: User;
}

/**
 * Who is signed in. Owners and staff sign in with their phone number and password, or with Google; a texted code
 * proves the number when signing up, linking Google, or resetting the password. The access token lasts 15 minutes and is
 * refreshed on demand with the refresh token; both are kept in localStorage so a page reload stays signed in.
 */
@Injectable({ providedIn: 'root' })
export class Session {
  private readonly http = inject(HttpClient);

  private readonly router = inject(Router);

  private readonly state = signal<StoredSession | null>(read());

  private refreshing: Observable<TokenResponse> | null = null;

  readonly user = computed(() => this.state()?.user ?? null);

  readonly signedIn = computed(() => this.state() !== null);

  accessToken(): string | null {
    return this.state()?.accessToken ?? null;
  }

  options(): Promise<AuthOptions> {
    return firstValueFrom(this.http.get<AuthOptions>('/api/v1/auth/options'));
  }

  requestCode(phone: string, purpose: CodePurpose): Promise<OtpSent> {
    return firstValueFrom(this.http.post<OtpSent>('/api/v1/auth/codes', { phone, purpose }));
  }

  signIn(phone: string, password: string): Promise<User> {
    return this.signInWith('/api/v1/auth/sign-in', { phone, password });
  }

  signUp(phone: string, code: string, fullName: string, password: string): Promise<User> {
    return this.signInWith('/api/v1/auth/sign-up', { phone, code, fullName, password });
  }

  /** Also how accounts from before passwords set their first one. Signs out every other device. */
  resetPassword(phone: string, code: string, password: string): Promise<User> {
    return this.signInWith('/api/v1/auth/password/reset', { phone, code, password });
  }

  /** {@code idToken} is the credential from "Sign in with Google"; GOOGLE_PHONE_NEEDED the first time. */
  google(idToken: string): Promise<User> {
    return this.signInWith('/api/v1/auth/google', { idToken });
  }

  /** A first Google sign-in, with a LINK_GOOGLE code. {@code fullName} only if Google didn't give a name. */
  linkGoogle(idToken: string, phone: string, code: string, fullName?: string): Promise<User> {
    return this.signInWith('/api/v1/auth/google/link', { idToken, phone, code, fullName: fullName || undefined });
  }

  /** Signs out every other device; this one gets a new session. */
  async changePassword(currentPassword: string | null, newPassword: string): Promise<void> {
    this.store(await firstValueFrom(this.http.put<TokenResponse>('/api/v1/me/password', { currentPassword, newPassword })));
  }

  /** Swaps the refresh token for a new pair; concurrent callers share one request. */
  refresh(): Observable<TokenResponse> {
    const refreshToken = this.state()?.refreshToken;
    if (!refreshToken) {
      throw new Error('Not signed in');
    }
    this.refreshing ??= this.http.post<TokenResponse>('/api/v1/auth/refresh', { refreshToken }).pipe(
      tap((tokens) => this.store(tokens)),
      finalize(() => (this.refreshing = null)),
      shareReplay(1),
    );
    return this.refreshing;
  }

  async signOut(): Promise<void> {
    const refreshToken = this.state()?.refreshToken;
    this.clear();
    if (refreshToken) {
      // Best effort: the session is gone locally either way
      this.http.post('/api/v1/auth/logout', { refreshToken }).subscribe({ error: () => undefined });
    }
    await this.router.navigateByUrl('/login');
  }

  /** The refresh token is no longer valid: start again at the login screen. */
  expired(): void {
    this.clear();
    void this.router.navigate(['/login'], { queryParams: { expired: 1 } });
  }

  private async signInWith(path: string, body: object): Promise<User> {
    const tokens = await firstValueFrom(this.http.post<TokenResponse>(path, body));
    this.store(tokens);
    return tokens.user;
  }

  private store(tokens: TokenResponse): void {
    const session = { accessToken: tokens.accessToken, refreshToken: tokens.refreshToken, user: tokens.user };
    this.state.set(session);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } catch {
      // Private browsing: stay signed in for this tab only
    }
  }

  private clear(): void {
    this.state.set(null);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // Nothing stored
    }
  }
}

function read(): StoredSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as StoredSession) : null;
  } catch {
    return null;
  }
}
