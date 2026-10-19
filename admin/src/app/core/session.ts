import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom, Observable, shareReplay, tap, finalize } from 'rxjs';

import { CodePurpose, OtpSent, TokenResponse, User } from './api.types';

const STORAGE_KEY = 'futslot.admin.session';

interface StoredSession {
  accessToken: string;
  refreshToken: string;
  user: User;
}

/**
 * Who is signed in, by phone number and password. A texted code resets the password, which is also how an admin
 * account from before passwords sets its first one. The access token lasts 15 minutes and is
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

  /** Signed in with a FutSlot admin account; the API refuses everything else here anyway. */
  readonly admin = computed(() => this.state()?.user.roles.includes('ADMIN') ?? false);

  accessToken(): string | null {
    return this.state()?.accessToken ?? null;
  }

  requestCode(phone: string, purpose: CodePurpose): Promise<OtpSent> {
    return firstValueFrom(this.http.post<OtpSent>('/api/v1/auth/codes', { phone, purpose }));
  }

  signIn(phone: string, password: string): Promise<User> {
    return this.signInWith('/api/v1/auth/sign-in', { phone, password });
  }

  /** Also how accounts from before passwords set their first one. Signs out every other device. */
  resetPassword(phone: string, code: string, password: string): Promise<User> {
    return this.signInWith('/api/v1/auth/password/reset', { phone, code, password });
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

  /** The player changed their name or details. */
  updateUser(user: User): void {
    const current = this.state();
    if (current) {
      this.persist({ ...current, user });
    }
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
    this.persist({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken, user: tokens.user });
  }

  private persist(session: StoredSession): void {
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
